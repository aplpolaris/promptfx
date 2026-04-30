/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.geminisdk

import com.google.genai.Client
import com.google.genai.types.*
import tri.ai.core.*
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
                Client.builder()
                    .apiKey(settings.apiKey)
                    .project(settings.projectId)
                    .location(settings.location)
                    .vertexAI(true)
                    .build()
            } else {
                Client.builder()
                    .apiKey(settings.apiKey)
                    .build()
            }
        }
    }

    fun isConfigured() = settings.isConfigured() && client != null

    fun listModels(): List<Model?> {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val listConfig = ListModelsConfig.builder().build()
        val models = genClient.models.list(listConfig)
        return models.toList()
    }

    /**
     * Generate content using the specified model with text prompt and optional history.
     */
    fun generateContent(
        modelId: String,
        variation: MChatVariation = MChatVariation(),
        history: List<MultimodalChatMessage> = emptyList(),
        tools: MChatTools? = null,
        numResponses: Int = 1
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val contents = buildContentList(history)
        val systemInstruction = history.firstOrNull { it.role == MChatRole.System }?.content?.first()?.text
        val config = buildGenerateContentConfig(systemInstruction, variation, tools, numResponses)
        return genClient.models.generateContent(modelId, contents, config)
    }

    private fun buildContentList(history: List<MultimodalChatMessage>): List<Content> {
        return history.filter { it.role != MChatRole.System }.map { msg ->
            Content.builder()
                .parts((msg.content ?: listOf()).map { it.toGeminiPart() })
                .role(msg.role.toGeminiRole())
                .build()
        }
    }

    private fun MChatMessagePart.toGeminiPart(): Part = when (partType) {
        MPartType.TEXT -> Part.fromText(text)
        MPartType.IMAGE -> parseDataUrlToPart(inlineData!!)
        MPartType.TOOL_CALL -> Part.fromFunctionCall(functionName, functionArgs)
        MPartType.TOOL_RESPONSE -> Part.fromFunctionResponse(functionName, functionArgs)
    }

    private fun parseDataUrlToPart(dataUrl: String): Part {
        require(dataUrl.startsWith("data:") && dataUrl.contains(";base64,")) {
            "Invalid data URL format. Expected format: data:<mimeType>;base64,<data>"
        }
        val mimeType = dataUrl.substringBefore(";base64,").substringAfter("data:")
        val base64Data = dataUrl.substringAfter(";base64,")
        val bytes = java.util.Base64.getDecoder().decode(base64Data)
        return Part.fromBytes(bytes, mimeType)
    }

    private fun buildGenerateContentConfig(
        systemInstruction: String?,
        variation: MChatVariation,
        tools: MChatTools?,
        numResponses: Int
    ): GenerateContentConfig {
        val builder = GenerateContentConfig.builder()
        variation.temperature?.let { builder.temperature(it.toFloat()) }
        variation.topP?.let { builder.topP(it.toFloat()) }
        variation.topK?.let { builder.topK(it.toFloat()) }
        if (numResponses > 1) builder.candidateCount(numResponses)
        systemInstruction?.let {
            builder.systemInstruction(Content.builder().parts(listOf(Part.fromText(it))).build())
        }
        tools?.let {
            builder.tools(it.tools.map { tool ->
                Tool.builder().functionDeclarations(listOf(
                    FunctionDeclaration.builder()
                        .name(tool.name)
                        .description(tool.description)
                        .parameters(Schema.fromJson(tool.jsonSchema))
                        .build()
                )).build()
            })
        }
        return builder.build()
    }

    private fun MChatRole.toGeminiRole(): String = when (this) {
        MChatRole.User -> "user"
        MChatRole.Assistant -> "model"
        else -> "user"
    }

    fun embedContent(content: String, modelId: String, outputDimensionality: Int? = null): List<Float> {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val embedConfig = EmbedContentConfig.builder()
            .let { if (outputDimensionality != null) it.outputDimensionality(outputDimensionality) else it }
            .build()
        return genClient.models.embedContent(modelId, content, embedConfig)
            .embeddings().get()
            .map { it.values().get() }
            .first()
    }

    fun batchEmbedContents(contents: List<String>, modelId: String, outputDimensionality: Int? = null): List<List<Float>> {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val embedConfig = EmbedContentConfig.builder()
            .let { if (outputDimensionality != null) it.outputDimensionality(outputDimensionality) else it }
            .build()
        return genClient.models.embedContent(modelId, contents, embedConfig)
            .embeddings().get()
            .map { it.values().get() }
    }

    /**
     * Generate content from audio bytes and a text prompt.
     */
    fun generateContentAudio(
        modelId: String,
        audioBytes: ByteArray,
        mimeType: String,
        prompt: String
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val contents = listOf(
            Content.builder()
                .parts(listOf(
                    Part.fromText(prompt),
                    Part.fromBytes(audioBytes, mimeType)
                ))
                .role("user")
                .build()
        )
        val config = GenerateContentConfig.builder().build()
        return genClient.models.generateContent(modelId, contents, config)
    }

    /**
     * Generate images using generateContent with IMAGE response modality (for gemini-*-image models).
     * Extracts inline image data from the response candidates.
     */
    fun generateContentImages(
        modelId: String,
        prompt: String,
        aspectRatio: String? = null,
        imageSize: String? = null
    ): GenerateContentResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val content = Content.builder()
            .parts(listOf(Part.fromText(prompt)))
            .role("user")
            .build()
        val imageConfigBuilder = ImageConfig.builder()
        if (aspectRatio != null) imageConfigBuilder.aspectRatio(aspectRatio)
        if (imageSize != null) imageConfigBuilder.imageSize(imageSize)
        val config = GenerateContentConfig.builder()
            .responseModalities("IMAGE", "TEXT")
            .imageConfig(imageConfigBuilder.build())
            .build()
        return genClient.models.generateContent(modelId, content, config)
    }

    /**
     * Generate images using the Imagen API (for imagen-* models).
     */
    fun generateImagesViaImagenApi(
        modelId: String,
        prompt: String,
        numberOfImages: Int = 1,
        aspectRatio: String? = null
    ): GenerateImagesResponse {
        val genClient = client ?: throw IllegalStateException("Client not initialized")
        val configBuilder = GenerateImagesConfig.builder()
            .numberOfImages(numberOfImages)
        if (aspectRatio != null) configBuilder.aspectRatio(aspectRatio)
        return genClient.models.generateImages(modelId, prompt, configBuilder.build())
    }

    override fun close() {
        // The java-genai Client handles connection pooling internally
    }

    companion object {
        val INSTANCE by lazy { GeminiSdkClient() }

        /** Extract text responses from a GenerateContentResponse. */
        fun GenerateContentResponse.extractTexts(numResponses: Int): List<String> {
            if (numResponses <= 1) return listOf(text() ?: "")
            
            val candidates = candidates()?.takeIf { it.isPresent }?.get() ?: return listOf(text() ?: "")
            
            val texts = candidates.mapNotNull { candidate ->
                extractTextFromCandidate(candidate)
            }
            return texts.ifEmpty { listOf(text() ?: "") }
        }

        private fun extractTextFromCandidate(candidate: Candidate): String? {
            val content = candidate.content().takeIf { it.isPresent }?.get() ?: return null
            val parts = content.parts().takeIf { it.isPresent }?.get() ?: return null
            val firstPart = parts.firstOrNull() ?: return null
            return firstPart.text()?.orElse(null)?.takeIf { it.isNotBlank() }
        }
    }

}
