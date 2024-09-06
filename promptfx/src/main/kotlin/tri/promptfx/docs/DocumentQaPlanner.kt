/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.promptfx.docs

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import tornadofx.observableListOf
import tornadofx.runLater
import tri.ai.core.TextCompletion
import tri.ai.embedding.*
import tri.ai.embedding.LocalFolderEmbeddingIndex.Companion.EMBEDDINGS_FILE_NAME_LEGACY
import tri.ai.openai.OpenAiModelIndex.ADA_ID
import tri.ai.openai.instructTask
import tri.ai.pips.aitask
import tri.ai.pips.task
import tri.ai.prompt.trace.*
import tri.ai.text.chunks.TextChunkInDoc
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.originalFile
import tri.ai.text.chunks.process.LocalFileManager.textCacheFile
import tri.ai.text.chunks.process.LocalTextDocIndex.Companion.createTextDoc
import tri.ai.text.chunks.process.TextDocEmbeddings.calculateMissingEmbeddings
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.ai.text.chunks.process.TextDocEmbeddings.putEmbeddingInfo
import tri.promptfx.ui.FormattedPromptTraceResult
import tri.promptfx.ui.FormattedText
import tri.promptfx.ui.FormattedTextNode
import tri.promptfx.ui.splitOn
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import tri.util.fine
import tri.util.info
import java.io.File

/** Runs the document QA information retrieval, query, and summarization process. */
class DocumentQaPlanner {

    /** A document library to use for chunks, if available. */
    var documentLibrary = SimpleObjectProperty<TextLibrary>(null)
    /** The embedding index. */
    var embeddingIndex: ObservableValue<out EmbeddingIndex?> = SimpleObjectProperty(NoOpEmbeddingIndex)
    /** The retrieved relevant snippets. */
    val snippets = observableListOf<EmbeddingMatch>()
    /** The most recent result of the QA task. */
    var lastResult: QuestionAnswerResult? = null

    /**
     * Asynchronous tasks to execute for answering the given question.
     * @param question question to answer
     * @param promptId prompt to use for answering the question
     * @param embeddingService embedding service to use
     * @param chunksToRetrieve number of chunks to retrieve
     * @param minChunkSize minimum size of a chunk for use in a prompt
     * @param contextStrategy strategy for constructing the context
     * @param contextChunks how many of the retrieved chunks to use for constructing the context
     * @param completionEngine completion engine to use
     * @param maxTokens maximum number of tokens to generate
     * @param temp temperature for sampling
     * @param numResponses number of responses to generate
     */
    fun plan(
        question: String,
        promptId: String,
        embeddingService: EmbeddingService,
        chunksToRetrieve: Int,
        minChunkSize: Int,
        contextStrategy: SnippetJoiner,
        contextChunks: Int,
        completionEngine: TextCompletion,
        maxTokens: Int,
        temp: Double,
        numResponses: Int
    ) = task("upgrade-embeddings-file") {
            runLater { snippets.setAll() }
            if (documentLibrary.value == null && embeddingIndex.value is LocalFolderEmbeddingIndex)
                upgradeEmbeddingIndex()
        }.task("load-embeddings-file-and-calculate") {
            embeddingIndex.value!!.findMostSimilar("a", 1)
        }.aitask("find-relevant-sections") {
            findRelevantSection(question, chunksToRetrieve).also {
                runLater { snippets.setAll(it.firstValue) }
            }
        }.aitasklist("question-answer") {
            val queryChunks = it.filter { it.chunkSize >= minChunkSize }
                .take(contextChunks)
            val context = contextStrategy.constructContext(queryChunks)
            val response = completionEngine.instructTask(promptId, question, context, maxTokens, temp, numResponses)
            val questionEmbedding = embeddingService.calculateEmbedding(question)
            val responseEmbeddings = response.values?.map {
                embeddingService.calculateEmbedding(it)
            } ?: listOf()
            // TODO - make this support more than one response embedding
            // add snippet response scores for first response embedding only
            if (responseEmbeddings.isNotEmpty()) {
                snippets.forEach {
                    it.responseScore = cosineSimilarity(responseEmbeddings[0], it.chunkEmbedding).toFloat()
                }
            }
            response.mapOutput {
                QuestionAnswerResult(
                    query = SemanticTextQuery(question, questionEmbedding, embeddingService.modelId),
                    matches = snippets,
                    trace = response,
                    responseEmbeddings = responseEmbeddings
                )
            }
        }.aitask("process-result") {
            info<DocumentQaPlanner>("$ANSI_GRAY  Similarity of question to response: ${it.responseScore}$ANSI_RESET")
            lastResult = it
            FormattedPromptTraceResult(it.trace, it.splitQaResults().map { formatResult(it) })
        }.planner

    //region SIMILARITY CALCULATIONS

    /** Finds the most relevant section to the query. */
    private suspend fun findRelevantSection(query: String, maxChunks: Int): AiPromptTrace<EmbeddingMatch> {
        documentLibrary.value?.let { return findRelevantSection(it, query, maxChunks) }
        val matches = embeddingIndex.value!!.findMostSimilar(query, maxChunks)
        val modelId = (embeddingIndex.value as? LocalFolderEmbeddingIndex)?.embeddingService?.modelId
        return AiPromptTrace(
            modelInfo = modelId?.let { AiModelInfo(it) },
            outputInfo = AiOutputInfo(matches)
        )
    }

    /** Finds the most relevant section to the query. */
    private suspend fun findRelevantSection(library: TextLibrary, query: String, maxChunks: Int): AiPromptTrace<EmbeddingMatch> {
        val embeddingSvc = (embeddingIndex.value as LocalFolderEmbeddingIndex).embeddingService
        val modelId = embeddingSvc.modelId
        val semanticTextQuery = SemanticTextQuery(query, embeddingSvc.calculateEmbedding(query), modelId)
        val matches = library.docs.flatMap { doc ->
            doc.calculateMissingEmbeddings(embeddingSvc)
            doc.chunks.map {
                val chunkEmbedding = it.getEmbeddingInfo(modelId)!!
                EmbeddingMatch(semanticTextQuery, doc, it, modelId, chunkEmbedding,
                    cosineSimilarity(semanticTextQuery.embedding, chunkEmbedding).toFloat()
                )
            }
        }.sortedByDescending { it.queryScore }.take(maxChunks)
        return AiPromptTrace(
            modelInfo = AiModelInfo(modelId),
            outputInfo = AiOutputInfo(matches)
        )
    }

    suspend fun reindexAllDocuments() {
        (embeddingIndex.value as? LocalFolderEmbeddingIndex)?.reindexAll()
    }

    //endregion

    //region FORMATTING RESULTS OF QA

    /** Split a QA result into multiple QA results, one per output. */
    private fun QuestionAnswerResult.splitQaResults(): List<QuestionAnswerResult> {
        return trace.values?.map { output ->
            val trace2 = trace.copy()
            trace2.output = AiOutputInfo(listOf(output))
            QuestionAnswerResult(query, matches, trace2, responseEmbeddings)
        } ?: listOf(this)
    }

    /** Formats the result of the QA task. */
    private fun formatResult(qaResult: QuestionAnswerResult): FormattedText {
        val result = mutableListOf(FormattedTextNode(qaResult.trace.values?.joinToString() ?: "No response."))
        val docs = qaResult.matches.mapNotNull { it.document.browsable() }
            .filter { it.shortNameWithoutExtension.isNotBlank() }
            .toSet()
        docs.sortedByDescending { it.shortNameWithoutExtension.length }.forEach { doc ->
            result.splitOn(doc.shortNameWithoutExtension) {
                FormattedTextNode(doc.shortNameWithoutExtension,
                    hyperlink = doc.file?.absolutePath ?: doc.uri.path)
            }
        }
        result.splitOn("Citations:") { FormattedTextNode(it, BOLD_STYLE) }
        // multiple possible citation formats
        result.splitOn("\\[[0-9]+(,\\s*[0-9]+)*]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        result.splitOn("\\[Citation [0-9]+(,\\s*[0-9]+)*]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        result.splitOn("\\[\\[[0-9]+(,\\s*[0-9]+)*]]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        result.splitOn("\\[\\[Citation [0-9]+(,\\s*[0-9]+)*]]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        return FormattedText(result)
    }

    //endregion

    //region WORKING WITH LEGACY DATA

    /** Upgrades a legacy format embeddings file. Only supports upgrading from OpenAI embeddings file, `embeddings.json`. */
    private fun upgradeEmbeddingIndex() {
        val index = embeddingIndex.value as LocalFolderEmbeddingIndex
        val folder = index.rootDir
        val file = index.indexFile
        val oldFile = File(folder, EMBEDDINGS_FILE_NAME_LEGACY)
        if (!file.exists() && oldFile.exists()) {
            fine<DocumentQaPlanner>("Checking legacy embeddings file for embedding vectors: $oldFile")
            try {
                var changed = false
                LegacyEmbeddingIndex.loadFrom(oldFile).info.values.map {
                    val f = LocalFileManager.fixPath(File(it.path), folder)?.originalFile()
                        ?: throw IllegalArgumentException("File not found: ${it.path}")
                    f.createTextDoc().apply {
                        all = TextChunkRaw(f.textCacheFile().readText())
                        chunks.addAll(it.sections.map {
                            TextChunkInDoc(it.start, it.end).apply {
                                if (it.embedding.isNotEmpty())
                                    putEmbeddingInfo(ADA_ID, it.embedding, EmbeddingPrecision.FIRST_EIGHT)
                            }
                        })
                    }
                }.forEach {
                    if (index.addIfNotPresent(it))
                        changed = true
                }
                if (changed) {
                    info<DocumentQaPlanner>("Upgraded legacy embeddings file to new format.")
                    index.saveIndex()
                    info<DocumentQaPlanner>("Legacy embeddings file $oldFile can be deleted unless needed for previous versions of PromptFx.")
                } else {
                    fine<DocumentQaPlanner>("No new embeddings found in legacy embeddings file.")
                }
            } catch (x: Exception) {
                info<DocumentQaPlanner>("Failed to load legacy embeddings file: ${x.message}")
            }
        }
    }

    //endregion

    companion object {
        private const val BOLD_STYLE = "-fx-font-weight: bold;"
        private const val LINK_STYLE = "-fx-fill: #8abccf; -fx-font-weight: bold;"
    }

}

//region DATA OBJECTS DESCRIBING TASK

/** Result object. */
data class QuestionAnswerResult(
    val query: SemanticTextQuery,
    val matches: List<EmbeddingMatch>,
    val trace: AiPromptTrace<String>,
    val responseEmbeddings: List<List<Double>>
) {
    override fun toString() = trace.output?.outputs?.joinToString() ?: "No response. Question: ${query.query}"

    /** Calculates the similarity between the question and response. */
    val responseScore
        get() = responseEmbeddings.map { cosineSimilarity(query.embedding, it).toFloat() } ?: 0f
}

//endregion