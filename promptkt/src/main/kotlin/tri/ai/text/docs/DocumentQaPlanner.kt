/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.text.docs

import tri.ai.core.TextChatMessage
import tri.ai.core.TextCompletion
import tri.ai.core.instructTask
import tri.ai.embedding.*
import tri.ai.pips.AiTaskList
import tri.ai.pips.task
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.trace.*
import tri.ai.text.chunks.SnippetJoiner
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import tri.util.info

/** Runs the document QA information retrieval, query, and summarization process. */
class DocumentQaPlanner(val index: EmbeddingIndex, val completionEngine: TextCompletion, val chatHistory: List<TextChatMessage>, val historySize: Int) {

    /**
     * Asynchronous tasks to execute for answering the given question.
     * @param question question to answer
     * @param prompt prompt to use for answering the question
     * @param chunksToRetrieve number of chunks to retrieve
     * @param minChunkSize minimum size of a chunk for use in a prompt
     * @param contextStrategy strategy for constructing the context
     * @param contextChunks how many of the retrieved chunks to use for constructing the context
     * @param maxTokens maximum number of tokens to generate
     * @param temp temperature for sampling
     * @param numResponses number of responses to generate
     */
    fun plan(
        question: String,
        prompt: AiPrompt,
        chunksToRetrieve: Int,
        minChunkSize: Int,
        contextStrategy: SnippetJoiner,
        contextChunks: Int,
        maxTokens: Int,
        temp: Double,
        numResponses: Int,
        snippetCallback: (List<EmbeddingMatch>) -> Unit
    ): AiTaskList<String> = task("upgrade-embeddings-file") {
        snippetCallback(emptyList())
        (index as? LocalFolderEmbeddingIndex)?.upgradeEmbeddingIndex()
    }.task("load-embeddings-file-and-calculate") {
        // trigger loading of embeddings file using a similarity query
        index.findMostSimilar("a", 1)
    }.aitask("find-relevant-sections") {
        // for each question, generate a list of relevant chunks
        findRelevantSection(question, chunksToRetrieve).also {
            snippetCallback(it.values!!)
        }
    }.aitaskonlist("question-answer") { snippets ->
        val queryChunks = snippets.filter { it.chunkSize >= minChunkSize }
            .take(contextChunks)
        val context = contextStrategy.constructContext(queryChunks)
        val response = completionEngine.instructTask(prompt, question, context, maxTokens, temp, numResponses,
            history = chatHistory.takeLast(historySize)
        )
        val questionEmbedding = index.embeddingService.calculateEmbedding(question)
        val responseEmbeddings = response.values?.map {
            index.embeddingService.calculateEmbedding(it)
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
                query = SemanticTextQuery(question, questionEmbedding, index.embeddingService.modelId),
                matches = snippets,
                trace = response,
                responseEmbeddings = responseEmbeddings
            )
        }
    }.aitask("process-result") {
        info<DocumentQaPlanner>("$ANSI_GRAY Similarity of question to response: ${it.responseScore}$ANSI_RESET")
        FormattedPromptTraceResult(it.trace, it.splitOutputs().map { it.formatResult() })
    }

    //region SIMILARITY CALCULATIONS

    /** Finds the most relevant section to the query. */
    private suspend fun findRelevantSection(query: String, maxChunks: Int): AiPromptTrace<EmbeddingMatch> {
//        documentLibrary?.let { return findRelevantSection(it, query, maxChunks) }
        val matches = index.findMostSimilar(query, maxChunks)
        val modelId = (index as? LocalFolderEmbeddingIndex)?.embeddingService?.modelId
        return AiPromptTrace(
            modelInfo = modelId?.let { AiModelInfo(it) },
            outputInfo = AiOutputInfo(matches)
        )
    }

//    /** Finds the most relevant section to the query. */
//    private suspend fun findRelevantSection(library: TextLibrary, query: String, maxChunks: Int): AiPromptTrace<EmbeddingMatch> {
//        val embeddingSvc = index.embeddingService
//        val modelId = embeddingSvc.modelId
//        val semanticTextQuery = SemanticTextQuery(query, embeddingSvc.calculateEmbedding(query), modelId)
//        val matches = library.docs.flatMap { doc ->
//            doc.calculateMissingEmbeddings(embeddingSvc)
//            doc.chunks.map {
//                val chunkEmbedding = it.getEmbeddingInfo(modelId)!!
//                EmbeddingMatch(semanticTextQuery, doc, it, modelId, chunkEmbedding,
//                    cosineSimilarity(semanticTextQuery.embedding, chunkEmbedding).toFloat()
//                )
//            }
//        }.sortedByDescending { it.queryScore }.take(maxChunks)
//        return AiPromptTrace(
//            modelInfo = AiModelInfo(modelId),
//            outputInfo = AiOutputInfo(matches)
//        )
//    }

    //endregion

}
