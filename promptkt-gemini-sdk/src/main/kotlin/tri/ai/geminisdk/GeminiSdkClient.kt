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
import com.google.genai.types.*
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.VisionLanguageChatMessage
import java.io.Closeable
import java.net.URI

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
     * Generate content using the specified model with text prompt and optional history.
     */
    suspend fun generateContent(
        prompt: String,
        modelId: String,
        variation: MChatVariation = MChatVariation(),
        history: List<TextChatMessage> = emptyList(),
        numResponses: Int = 1
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        
        // Build generation config from variation
        val generationConfig = buildGenerationConfig(variation, numResponses)
        
        // Build content list with history and current prompt
        val contents = buildContentList(history, prompt)
        
        // Extract system instruction if present
        val systemInstruction = history.firstOrNull { it.role == MChatRole.System }?.content?.let {
            Content.newBuilder()
                .addParts(Part.newBuilder().setText(it).build())
                .build()
        }
        
        val configBuilder = GenerateContentConfig.newBuilder()
            .setGenerationConfig(generationConfig)
        
        systemInstruction?.let { configBuilder.setSystemInstruction(it) }
        val config = configBuilder.build()
        
        return genClient.models.generateContent(modelId, contents, config)
    }

    /**
     * Generate content with vision/image support.
     */
    suspend fun generateContentVision(
        messages: List<VisionLanguageChatMessage>,
        modelId: String,
        variation: MChatVariation = MChatVariation(),
        numResponses: Int = 1
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        
        // Build generation config
        val generationConfig = buildGenerationConfig(variation, numResponses)
        
        // Build content list with images
        val contents = messages.filter { it.role != MChatRole.System }.map { msg ->
            val contentBuilder = Content.newBuilder()
            
            // Add text part
            if (!msg.content.isNullOrBlank()) {
                contentBuilder.addParts(Part.newBuilder().setText(msg.content).build())
            }
            
            // Add image part if present
            msg.image?.let { imageUri ->
                val imagePart = createImagePart(imageUri)
                contentBuilder.addParts(imagePart)
            }
            
            contentBuilder.setRole(msg.role.toGeminiRole())
            contentBuilder.build()
        }
        
        // Extract system instruction
        val systemInstruction = messages.firstOrNull { it.role == MChatRole.System }?.content?.let {
            Content.newBuilder()
                .addParts(Part.newBuilder().setText(it).build())
                .build()
        }
        
        val configBuilder = GenerateContentConfig.newBuilder()
            .setGenerationConfig(generationConfig)
        
        systemInstruction?.let { configBuilder.setSystemInstruction(it) }
        val config = configBuilder.build()
        
        return genClient.models.generateContent(modelId, contents, config)
    }

    /**
     * Build generation config from variation parameters.
     */
    private fun buildGenerationConfig(variation: MChatVariation, numResponses: Int): GenerationConfig {
        val builder = GenerationConfig.newBuilder()
        
        variation.temperature?.let { builder.setTemperature(it.toFloat()) }
        variation.topP?.let { builder.setTopP(it.toFloat()) }
        variation.topK?.let { builder.setTopK(it.toInt()) }
        
        // Set candidate count for multiple responses
        if (numResponses > 1) {
            builder.setCandidateCount(numResponses)
        }
        
        return builder.build()
    }

    /**
     * Build content list from history and current prompt.
     */
    private fun buildContentList(history: List<TextChatMessage>, prompt: String): List<Content> {
        val contents = mutableListOf<Content>()
        
        // Add history (excluding system messages)
        history.filter { it.role != MChatRole.System }.forEach { msg ->
            contents.add(Content.newBuilder()
                .addParts(Part.newBuilder().setText(msg.content).build())
                .setRole(msg.role.toGeminiRole())
                .build())
        }
        
        // Add current prompt as user message
        contents.add(Content.newBuilder()
            .addParts(Part.newBuilder().setText(prompt).build())
            .setRole("user")
            .build())
        
        return contents
    }

    /**
     * Create image part from URI (supports data URLs).
     */
    private fun createImagePart(imageUri: URI): Part {
        val urlStr = imageUri.toString()
        
        return if (urlStr.startsWith("data:")) {
            // Parse data URL
            val mimeType = urlStr.substringBefore(";base64,").substringAfter("data:")
            val base64Data = urlStr.substringAfter(";base64,")
            
            val blob = Blob.newBuilder()
                .setData(base64Data)
                .setMimeType(mimeType)
                .build()
                
            Part.newBuilder()
                .setInlineData(blob)
                .build()
        } else {
            // For non-data URLs, we would need to fetch and encode, but for now just throw
            throw UnsupportedOperationException("Only data URLs are currently supported for images. Got: $urlStr")
        }
    }

    /**
     * Convert MChatRole to Gemini role string.
     */
    private fun MChatRole.toGeminiRole(): String = when (this) {
        MChatRole.User -> "user"
        MChatRole.Assistant -> "model"
        MChatRole.System -> "user" // System messages are handled separately
        else -> "user" // Default for any other types
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
