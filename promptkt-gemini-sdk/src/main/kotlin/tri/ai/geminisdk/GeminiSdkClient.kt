/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.geminisdk

import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.GenerateContentResponse
import com.google.cloud.vertexai.generativeai.ChatSession
import com.google.cloud.vertexai.generativeai.GenerativeModel
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import java.io.Closeable

/**
 * Client for the Gemini API using Google's official Java SDK.
 * See https://github.com/googleapis/java-genai
 */
class GeminiSdkClient : Closeable {

    val settings = GeminiSdkSettings()
    private var vertexAI: VertexAI? = null

    init {
        if (settings.isConfigured()) {
            vertexAI = VertexAI.Builder()
                .setProjectId(settings.projectId)
                .setLocation(settings.location)
                .build()
        }
    }

    /** Returns true if the client is configured with required settings. */
    fun isConfigured() = settings.isConfigured() && vertexAI != null

    /**
     * Generate content using the specified model.
     */
    suspend fun generateContent(
        prompt: String,
        modelId: String,
        variation: MChatVariation = MChatVariation(),
        history: List<TextChatMessage> = emptyList()
    ): GenerateContentResponse {
        val model = createModel(modelId, variation)
        
        return if (history.isEmpty()) {
            model.generateContent(prompt)
        } else {
            val chatSession = createChatSession(model, history)
            chatSession.sendMessage(prompt)
        }
    }

    /**
     * Generate embeddings for the given texts.
     * Note: The official SDK's embedding API is currently not implemented.
     * This will be added in a future update once the proper SDK API is available.
     */
    suspend fun embedContents(contents: List<String>, modelId: String): List<List<Float>> {
        throw NotImplementedError(
            "Embedding generation is not yet implemented for the Gemini SDK. " +
            "The official SDK does not expose the embedding API in the same way as the REST API. " +
            "Please use the promptkt-gemini plugin (REST API) for embedding functionality."
        )
    }

    private fun createModel(modelId: String, variation: MChatVariation): GenerativeModel {
        val ai = vertexAI ?: throw IllegalStateException("VertexAI not initialized")
        val builder = GenerativeModel.Builder()
            .setModelName(modelId)
            .setVertexAi(ai)
        
        // Apply generation config if variation is specified
        val configBuilder = com.google.cloud.vertexai.api.GenerationConfig.newBuilder()
        variation.temperature?.let { configBuilder.temperature = it.toFloat() }
        variation.topP?.let { configBuilder.topP = it.toFloat() }
        variation.topK?.let { configBuilder.topK = it.toFloat() }
        
        builder.setGenerationConfig(configBuilder.build())
        
        return builder.build()
    }

    private fun createChatSession(model: GenerativeModel, history: List<TextChatMessage>): ChatSession {
        // Start a new chat session
        val chatSession = model.startChat()
        
        // Note: The SDK's chat session doesn't easily support pre-populating history
        // For now, we'll just use the current prompt with the system instruction
        // which was already set in createModel if a System message was in history
        
        return chatSession
    }

    override fun close() {
        vertexAI?.close()
    }

    companion object {
        val INSTANCE by lazy { GeminiSdkClient() }
    }

}
