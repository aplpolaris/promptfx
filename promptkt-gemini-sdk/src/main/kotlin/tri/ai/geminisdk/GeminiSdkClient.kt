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

import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.GenerationConfig
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatVariation
import java.io.Closeable

/**
 * Client for the Gemini API using Google's official java-genai library.
 * See https://github.com/googleapis/java-genai
 */
class GeminiSdkClient : Closeable {

    val settings = GeminiSdkSettings()
    private var client: Client? = null

    init {
        if (settings.isConfigured()) {
            client = if (settings.useVertexAI) {
                // Use Vertex AI mode with project and location
                Client.builder()
                    .apiKey(settings.apiKey)
                    .project(settings.projectId)
                    .location(settings.location)
                    .vertexAI(true)
                    .build()
            } else {
                // Use Gemini Developer API mode
                Client.builder()
                    .apiKey(settings.apiKey)
                    .build()
            }
        }
    }

    /** Returns true if the client is configured with required settings. */
    fun isConfigured() = settings.isConfigured() && client != null

    /**
     * Generate content using the specified model.
     */
    suspend fun generateContent(
        prompt: String,
        modelId: String,
        variation: MChatVariation = MChatVariation(),
        history: List<TextChatMessage> = emptyList()
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        
        // Build generation config from variation
        val generationConfig = GenerationConfig.builder().apply {
            variation.temperature?.let { temperature(it) }
            variation.topP?.let { topP(it) }
            variation.topK?.let { topK(it) }
        }.build()
        
        val config = GenerateContentConfig.builder()
            .generationConfig(generationConfig)
            .build()
        
        // For now, we'll use the simple text-based generation
        // History support can be added later with multi-turn conversations
        return genClient.models.generateContent(modelId, prompt, config)
    }

    /**
     * Generate embeddings for the given texts.
     * Note: The java-genai SDK's embedding API needs to be checked for availability.
     */
    suspend fun embedContents(contents: List<String>, modelId: String): List<List<Float>> {
        throw NotImplementedError(
            "Embedding generation is not yet implemented for the java-genai SDK. " +
            "The SDK's embedding API support needs to be verified. " +
            "Please use the promptkt-gemini plugin (REST API) for embedding functionality."
        )
    }

    override fun close() {
        // The java-genai Client doesn't have an explicit close method in current versions
        // Connection pooling is handled internally
    }

    companion object {
        val INSTANCE by lazy { GeminiSdkClient() }
    }

}
