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

import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.embedding.*
import tri.ai.pips.AiTaskList
import tri.ai.pips.task
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
    ): AiTaskList = task("load-embeddings-file-and-calculate") {
        // trigger loading of embeddings file using a similarity query, the result is ignored
        AiOutput(other = index.findMostSimilar("a", 1))
    }.aitask("find-relevant-sections") {
        // for each question, generate a list of relevant chunks
        findRelevantSection(question, chunksToRetrieve).also {
            snippetCallback(it.firstValue.content() as List<EmbeddingMatch>)
        }
    }.aitask("question-answer") { output ->
        val snippets = output.other as List<EmbeddingMatch>
        val queryChunks = snippets.filter { it.chunkSize >= minChunkSize }
            .take(contextChunks)
        val context = contextStrategy.constructContext(queryChunks)
        val query = prompt.template().fillInstruct(input = context, instruct = question)
        val messages = chatHistory.takeLast(historySize) + TextChatMessage.user(query)
        val response = chat.chat(messages, MChatVariation.temp(temp), maxTokens, null, numResponses, null)
        val embeddingModel = index.embeddingStrategy.model
        val questionEmbedding = embeddingModel.calculateEmbedding(question)
        val responseEmbeddings = response.values?.map {
            embeddingModel.calculateEmbedding(it.textContent())
        } ?: listOf()
        // TODO - make this support more than one response embedding
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
        response.mapOutput {
            AiOutput(other = QuestionAnswerResult(
                query = SemanticTextQuery(question, questionEmbedding, embeddingModel.modelId),
                matches = snippets,
                trace = response.mapOutput { AiOutput(text = it.textContent()) },
                responseEmbeddings = responseEmbeddings
            ))
        }
    }.aitask("process-result") {
        val result = it.content() as QuestionAnswerResult
        info<DocumentQaPlanner>("$ANSI_GRAY Similarity of question to response: ${result.responseScore}$ANSI_RESET")
        FormattedPromptTraceResult(result.trace, result.splitOutputs().map { it.formatResult() })
    }

    //region SIMILARITY CALCULATIONS

    /**
     * Finds the most relevant section to the query.
     * Result is encoded as a list of [EmbeddingMatch] in a single [AiOutput].
     */
    private suspend fun findRelevantSection(query: String, maxChunks: Int): AiPromptTrace {
        val matches = index.findMostSimilar(query, maxChunks)
        val modelId = (index as? LocalFolderEmbeddingIndex)?.embeddingStrategy?.modelId
        return AiPromptTrace(
            modelInfo = modelId?.let { AiModelInfo(it) },
            outputInfo = AiOutputInfo.listSingleOutput(matches)
        )
    }

    //endregion

}
