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

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import tornadofx.observableListOf
import tornadofx.runLater
import tri.ai.core.TextCompletion
import tri.ai.embedding.*
import tri.ai.openai.instructTask
import tri.ai.pips.AiTaskResult
import tri.ai.pips.aitask
import tri.ai.text.chunks.BrowsableSource
import tri.promptfx.ModelParameters
import tri.util.info

/** Runs the document QA information retrieval, query, and summarization process. */
class DocumentQaPlanner {

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
     * @param tempParameters temperature/randomness parameters
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
        tempParameters: ModelParameters
    ) = aitask("calculate-embeddings") {
        runLater { snippets.setAll() }
        findRelevantSection(question, chunksToRetrieve).also {
            runLater { snippets.setAll(it.value) }
        }
    }.aitask("question-answer") {
        val queryChunks = it.filter { it.chunkSize >= minChunkSize }
            .take(contextChunks)
        val context = contextStrategy.constructContext(queryChunks)
        val response = completionEngine.instructTask(promptId, question, context, maxTokens, tempParameters.temp.value)
        val questionEmbedding = embeddingService.calculateEmbedding(question)
        val responseEmbedding = response.value?.let { embeddingService.calculateEmbedding(it) }
        response.map {
            QuestionAnswerResult(
                modelId = completionEngine.modelId,
                promptId = promptId,
                query = SemanticTextQuery(question, questionEmbedding, embeddingService.modelId),
                matches = snippets,
                response = response.value,
                responseEmbedding = responseEmbedding
            )
        }
    }.task("process-result") {
        info<DocumentQaPlanner>("Similarity of question to response: " + it.questionAnswerSimilarity())
        lastResult = it
        formatResult(it)
    }.planner

    //region SIMILARITY CALCULATIONS

    /** Finds the most relevant section to the query. */
    private suspend fun findRelevantSection(query: String, maxChunks: Int): AiTaskResult<List<EmbeddingMatch>> {
        val matches = embeddingIndex.value!!.findMostSimilar(query, maxChunks)
        return AiTaskResult.result(matches)
    }

    suspend fun reindexAllDocuments() {
        (embeddingIndex.value as? LocalFolderEmbeddingIndex)?.reindexAll()
    }

    //endregion

    //region FORMATTING RESULTS OF QA

    private val BOLD_STYLE = "-fx-font-weight: bold;"
    private val LINK_STYLE = "-fx-fill: #8abccf; -fx-font-weight: bold;"

    /** Formats the result of the QA task. */
    private fun formatResult(qaResult: QuestionAnswerResult): FormattedText {
        val result = mutableListOf(FormattedTextNode(qaResult.response ?: "No response."))
        val docs = qaResult.matches.map { it.document.browsable()!! }.toSet()
        docs.forEach { doc ->
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

}

//region DATA OBJECTS DESCRIBING TASK

/** Result object. */
data class QuestionAnswerResult(
    val modelId: String,
    val promptId: String?,
    val query: SemanticTextQuery,
    val matches: List<EmbeddingMatch>,
    val response: String?,
    val responseEmbedding: List<Double>?
) {
    override fun toString() = response ?: "No response. Question: ${query.query}"

    /** Calculates the similarity between the question and response. */
    internal fun questionAnswerSimilarity() = responseEmbedding?.let { cosineSimilarity(query.embedding, it) } ?: 0
}