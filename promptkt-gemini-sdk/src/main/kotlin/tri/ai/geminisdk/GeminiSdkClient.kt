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
import tri.ai.core.MChatMessagePart
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChatMessage
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
    fun generateContent(
        modelId: String,
        variation: MChatVariation = MChatVariation(),
        history: List<MultimodalChatMessage> = emptyList(),
        numResponses: Int = 1
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val contents = buildContentList(history)
        val systemInstruction = history.firstOrNull { it.role == MChatRole.System }?.content?.first()?.text
        val config = generateContentConfig(systemInstruction, variation, numResponses)
        return genClient.models.generateContent(modelId, contents, config)
    }

    /**
     * Generate content with vision/image support.
     */
    fun generateContentVision(
        messages: List<VisionLanguageChatMessage>,
        modelId: String,
        variation: MChatVariation = MChatVariation(),
        numResponses: Int = 1
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        
        // Build content list with images
        val contents = messages.filter { it.role != MChatRole.System }.map { msg ->
            val parts = mutableListOf<Part>()
            if (msg.content.isNotBlank()) {
                parts.add(Part.fromText(msg.content))
            }
            parts.add(createImagePart(msg.image))

            Content.builder()
                .parts(parts)
                .role(msg.role.toGeminiRole())
                .build()
        }

        val systemInstruction = messages.firstOrNull { it.role == MChatRole.System }?.content
        val config = generateContentConfig(systemInstruction, variation, numResponses)
        return genClient.models.generateContent(modelId, contents, config)
    }

    /**
     * Build content list from history.
     */
    private fun buildContentList(history: List<MultimodalChatMessage>): List<Content> {
        val contents = mutableListOf<Content>()
        history.filter { it.role != MChatRole.System }.forEach { msg ->
            contents.add(Content.builder()
                .parts((msg.content ?: listOf()).map { it.gemini() })
                .role(msg.role.toGeminiRole())
                .build())
        }
        return contents
    }

    private fun MChatMessagePart.gemini(): Part = when (partType) {
        MPartType.TEXT -> Part.fromText(text)
        MPartType.IMAGE -> Part.fromBytes(inlineData!!.toByteArray(), "image/png") // assuming PNG; adjust as needed
        MPartType.TOOL_CALL -> Part.fromFunctionCall(functionName, functionArgs)
        MPartType.TOOL_RESPONSE -> Part.fromFunctionResponse(functionName, functionArgs)
    }

    /** Generate content config. */
    private fun generateContentConfig(
        systemInstruction: String?,
        variation: MChatVariation,
        numResponses: Int
    ): GenerateContentConfig {
        val builder = GenerateContentConfig.builder()
        variation.temperature?.let { builder.temperature(it.toFloat()) }
        variation.topP?.let { builder.topP(it.toFloat()) }
        variation.topK?.let { builder.topK(it.toFloat()) }
        if (numResponses > 1) {
            builder.candidateCount(numResponses)
        }
        systemInstruction?.let {
            val instruction = Content.builder()
                .parts(listOf(Part.fromText(it)))
                .build()
            builder.systemInstruction(instruction)
        }
        return builder.build()
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
            
            // Decode base64 to bytes
            val bytes = java.util.Base64.getDecoder().decode(base64Data)
                
            Part.fromBytes(bytes, mimeType)
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
    fun embedContents(contents: List<String>, modelId: String): List<List<Float>> {
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
