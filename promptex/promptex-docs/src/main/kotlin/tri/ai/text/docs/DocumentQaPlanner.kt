/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.embedding.*
import tri.ai.pips.AiTaskBuilder
import tri.ai.pips.progressUpdate
import tri.ai.prompt.PromptDef
import tri.ai.prompt.template
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.AiModelInfo.Companion.CHUNKER_ID
import tri.ai.prompt.trace.AiModelInfo.Companion.CHUNKER_MAX_CHUNK_SIZE
import tri.ai.prompt.trace.AiModelInfo.Companion.EMBEDDING_MODEL
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import tri.util.info

/** Runs the document QA information retrieval, query, and summarization process. */
class DocumentQaPlanner(val index: EmbeddingIndex, val chat: TextChat, val chatHistory: List<TextChatMessage>, val historySize: Int) {

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
        prompt: PromptDef,
        chunksToRetrieve: Int,
        minChunkSize: Int,
        contextStrategy: SnippetJoiner,
        contextChunks: Int,
        maxTokens: Int,
        temp: Double,
        numResponses: Int,
        snippetCallback: (List<EmbeddingMatch>) -> Unit
    ) = AiTaskBuilder.task("load-embeddings-file-and-calculate") { context ->
        // trigger loading of embeddings file (with progress), the result is ignored
        val progressScope = CoroutineScope(currentCoroutineContext() + Job())
        index.onProgress = { msg, pct -> progressScope.launch { context.monitor.progressUpdate(msg, pct) } }
        try {
            index.findMostSimilar("a", 1)
        } finally {
            index.onProgress = null
            progressScope.cancel()
        }
    }.task<List<EmbeddingMatch>>("find-relevant-sections") { loadResult, context ->
        // pass the load-step matches to the UI callback, then retrieve the real matches for this question
        snippetCallback(loadResult)
        val matches = index.findMostSimilar(question, chunksToRetrieve)
        val modelId = (index as? LocalFolderEmbeddingIndex)?.embeddingStrategy?.modelId
        context.logTrace("find-relevant-sections", AiPromptTrace(
            modelInfo = modelId?.let { AiModelInfo(it) },
            outputInfo = AiOutputInfo.listSingleOutput(matches)
        ))
        matches
    }.task<QuestionAnswerResult>("question-answer") { snippets, context ->
        val queryChunks = snippets.filter { it.chunkSize >= minChunkSize }.take(contextChunks)
        val ctx = contextStrategy.constructContext(queryChunks)
        val query = prompt.template().fillInstruct(input = ctx, instruct = question)
        val messages = chatHistory.takeLast(historySize) + TextChatMessage.user(query)
        val response = chat.chat(messages, MChatVariation.temp(temp), maxTokens, null, numResponses, null)
        val embeddingModel = index.embeddingStrategy.model
        val questionEmbedding = embeddingModel.calculateEmbedding(question)
        val responseEmbeddings = response.values?.map {
            embeddingModel.calculateEmbedding(it.textContent())
        } ?: listOf()
        // add snippet response scores for first response embedding only
        if (responseEmbeddings.isNotEmpty()) {
            snippets.forEach {
                it.responseScore = cosineSimilarity(responseEmbeddings[0], it.chunkEmbedding).toFloat()
            }
        }
        val model = response.model ?: AiModelInfo(chat.modelId)
        model.modelParams = (response.model?.modelParams ?: mapOf()) + mapOf<String, Any>(
            EMBEDDING_MODEL to embeddingModel.modelId,
            CHUNKER_ID to "PromptFx",
            CHUNKER_MAX_CHUNK_SIZE to ((index as? LocalFolderEmbeddingIndex)?.maxChunkSize ?: -1),
        )
        val result = QuestionAnswerResult(
            query = SemanticTextQuery(question, questionEmbedding, embeddingModel.modelId),
            matches = snippets,
            trace = response.mapOutput { AiOutput(text = it.textContent()) },
            responseEmbeddings = responseEmbeddings
        )
        context.logTrace("question-answer", response.mapOutput { AiOutput(other = result) })
        result
    }.task<FormattedPromptTraceResult>("process-result") { result, context ->
        info<DocumentQaPlanner>("$ANSI_GRAY Similarity of question to response: ${result.responseScore}$ANSI_RESET")
        val ft = FormattedPromptTraceResult(result.trace, result.splitOutputs().map { it.formatResult() })
        context.logTrace("process-result", ft)
        ft
    }

}
