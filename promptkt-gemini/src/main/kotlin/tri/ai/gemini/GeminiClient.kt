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
package tri.ai.gemini

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.VisionLanguageChatMessage
import java.io.Closeable

/**
 * General purpose client for the Gemini API.
 * See https://ai.google.dev/api?lang=web
 */
class GeminiClient : Closeable {

    val settings = GeminiSettings()
    private val client = settings.client

    /** Returns true if the client is configured with an API key. */
    fun isConfigured() = settings.apiKey.isNotBlank()

    //region CORE API METHODS

    suspend fun listModels(): ModelsResponse {
        return client.get("models")
            .body<ModelsResponse>()
    }

    suspend fun generateContent(modelId: String, request: GenerateContentRequest): GenerateContentResponse {
        return client.post("models/$modelId:generateContent") {
            setBody(request)
        }.body<GenerateContentResponse>()
    }

    //endregion

    //region ALTERNATE API METHODS

    suspend fun embedContent(content: String, modelId: String, outputDimensionality: Int? = null): EmbedContentResponse {
        val request = EmbedContentRequest(Content.text(content), outputDimensionality = outputDimensionality)
        return client.post("models/$modelId:embedContent") {
            setBody(request)
        }.body<EmbedContentResponse>()
    }

    suspend fun batchEmbedContents(content: List<String>, modelId: String, outputDimensionality: Int? = null): BatchEmbedContentsResponse {
        val request = BatchEmbedContentRequest(
            content.map { EmbedContentRequest(Content.text(it), model = "models/$modelId", outputDimensionality = outputDimensionality) }
        )
        return client.post("models/$modelId:batchEmbedContents") {
            setBody(request)
        }.body<BatchEmbedContentsResponse>()
    }

    suspend fun generateContent(prompt: String, modelId: String, variation: MChatVariation, numResponses: Int? = null, history: List<TextChatMessage>): GenerateContentResponse {
        val system = history.lastOrNull { it.role == MChatRole.System }?.content
        val request = GenerateContentRequest(
            contents = history.filter { it.role != MChatRole.System }.map {
                val role = it.role.toGeminiRole()
                Content(listOf(Part.text(it.content)), role)
            } + Content.text(prompt),
            systemInstruction = system?.let { Content(listOf(Part.text(it)), ContentRole.user) },
            generationConfig = variation.asGenerationConfig(numResponses = numResponses)
        )
        return generateContent(modelId, request)
    }

    private fun MChatVariation.asGenerationConfig(numResponses: Int? = null): GenerationConfig {
        return GenerationConfig(
            temperature = temperature,
            topP = topP,
            topK = topK,
            presencePenalty = presencePenalty,
            frequencyPenalty = frequencyPenalty,
            candidateCount = numResponses ?: 1
        )
    }

    suspend fun generateContent(prompt: String, image: String, modelId: String, numResponses: Int? = null, history: List<TextChatMessage>): GenerateContentResponse {
        val system = history.lastOrNull { it.role == MChatRole.System }?.content
        val request = GenerateContentRequest(
            contents = history.filter { it.role != MChatRole.System }.map {
                val role = it.role.toGeminiRole()
                Content(listOf(Part.text(it.content)), role)
            } + Content(listOf(
                Part.text(prompt),
                Part.inlineData(image, MIME_TYPE_JPEG)
            )),
            systemInstruction = system?.let { Content(listOf(Part.text(it)), ContentRole.user) },
// TODO - enable when Gemini API supports candidateCount, see https://ai.google.dev/api/generate-content#v1beta.GenerationConfig
//            generationConfig = numResponses?.let { GenerationConfig(candidateCount = it) }
        )
        return generateContent(modelId, request)
    }

    suspend fun generateContent(messages: List<TextChatMessage>, modelId: String, config: GenerationConfig? = null): GenerateContentResponse {
        val system = messages.lastOrNull { it.role == MChatRole.System }?.content
        val request = GenerateContentRequest(
            messages.filter { it.role != MChatRole.System }.map {
                val role = it.role.toGeminiRole()
                Content(listOf(Part.text(it.content)), role)
            },
            systemInstruction = system?.let { Content(listOf(Part.text(it)), ContentRole.user) },
            generationConfig = config
        )
        return generateContent(modelId, request)
    }

    suspend fun generateContentVision(messages: List<VisionLanguageChatMessage>, modelId: String, config: GenerationConfig? = null): GenerateContentResponse {
        val system = messages.lastOrNull { it.role == MChatRole.System }?.content
        val request = GenerateContentRequest(
            messages.filter { it.role != MChatRole.System }.map {
                val role = it.role.toGeminiRole()
                Content(listOf(
                    Part.text(it.content),
                    Part(inlineData = Blob.fromDataUrl(it.image))
                ), role)
            },
            systemInstruction = system?.let { Content(listOf(Part.text(it)), ContentRole.user) }, // TODO - support for system messages
            generationConfig = config
        )
        return generateContent(modelId, request)
    }

    //endregion

    override fun close() {
        client.close()
    }

    companion object {
        val INSTANCE by lazy { GeminiClient() }

        /** Convert from [MChatRole] to string representing Gemini role. */
        fun MChatRole.toGeminiRole() = when (this) {
            MChatRole.User -> ContentRole.user
            MChatRole.Assistant -> ContentRole.model
            else -> error("Invalid role: $this")
        }

        /** Convert from string representing Gemini role to [MChatRole]. */
        fun ContentRole?.fromGeminiRole() = when (this) {
            ContentRole.user -> MChatRole.User
            ContentRole.model -> MChatRole.Assistant
            else -> error("Invalid Gemini role: $this")
        }
    }

}

//region DTO's - see https://ai.google.dev/api?lang=web

@Serializable
data class ModelsResponse(
    val models: List<ModelInfo>
)

@Serializable
data class ModelInfo(
    val name: String,
    val baseModelId: String? = null, // though marked as required, not returned by API
    val version: String,
    val displayName: String,
    val description: String? = null, // though marked as required, not always returned by API
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val supportedGenerationMethods: List<String>,
    val thinking: Boolean? = null,
    val temperature: Double? = null,
    val maxTemperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null
)

//endregion
