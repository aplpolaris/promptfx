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
package tri.ai.gemini

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.core.VisionLanguageChatMessage
import java.io.Closeable
import java.net.URI

/**
 * General purpose client for the Gemini API.
 * See https://ai.google.dev/api?lang=web
 */
class GeminiClient : Closeable {

    private val settings = GeminiSettings()
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
        val request = EmbedContentRequest(Content(listOf(Part(content))), outputDimensionality = outputDimensionality)
        return client.post("models/$modelId:embedContent") {
            setBody(request)
        }.body<EmbedContentResponse>()
    }

    suspend fun batchEmbedContents(content: List<String>, modelId: String, outputDimensionality: Int? = null): BatchEmbedContentsResponse {
        val request = BatchEmbedContentRequest(
            content.map { EmbedContentRequest(Content(listOf(Part(it))), model = "models/$modelId", outputDimensionality = outputDimensionality) }
        )
        return client.post("models/$modelId:batchEmbedContents") {
            setBody(request)
        }.body<BatchEmbedContentsResponse>()
    }

    suspend fun generateContent(prompt: String, modelId: String, numResponses: Int? = null) =
        generateContent(modelId, GenerateContentRequest(
            content = Content.text(prompt),
// TODO - enable when Gemini API supports candidateCount, see https://ai.google.dev/api/generate-content#v1beta.GenerationConfig
//            generationConfig = numResponses?.let { GenerationConfig(candidateCount = it) }
        ))

    suspend fun generateContent(prompt: String, image: String, modelId: String, numResponses: Int? = null): GenerateContentResponse {
        val request = GenerateContentRequest(
            content = Content(listOf(
                Part(text = prompt),
                Part(inlineData = Blob(image, "image/jpeg"))
            )),
// TODO - enable when Gemini API supports candidateCount, see https://ai.google.dev/api/generate-content#v1beta.GenerationConfig
//            generationConfig = numResponses?.let { GenerationConfig(candidateCount = it) }
        )
        return generateContent(modelId, request)
    }

    suspend fun generateContent(messages: List<TextChatMessage>, modelId: String, config: GenerationConfig? = null): GenerateContentResponse {
        val system = messages.lastOrNull { it.role == TextChatRole.System }?.content
        val request = GenerateContentRequest(
            messages.filter { it.role != TextChatRole.System }.map {
                val role = it.role.toGeminiRole()
                Content(listOf(Part(it.content)), role)
            },
            systemInstruction = system?.let { Content(listOf(Part(it)), GEMINI_ROLE_USER) },
            generationConfig = config
        )
        return generateContent(modelId, request)
    }

    suspend fun generateContentVision(messages: List<VisionLanguageChatMessage>, modelId: String, config: GenerationConfig? = null): GenerateContentResponse {
        val system = messages.lastOrNull { it.role == TextChatRole.System }?.content
        val request = GenerateContentRequest(
            messages.filter { it.role != TextChatRole.System }.map {
                val role = it.role.toGeminiRole()
                Content(listOf(
                    Part(it.content),
                    Part(null, Blob.image(it.image))
                ), role)
            },
            systemInstruction = system?.let { Content(listOf(Part(it)), GEMINI_ROLE_USER) }, // TODO - support for system messages
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

        /** Convert from [TextChatRole] to string representing Gemini role. */
        fun TextChatRole.toGeminiRole() = when (this) {
            TextChatRole.User -> GEMINI_ROLE_USER
            TextChatRole.Assistant -> GEMINI_ROLE_MODEL
            else -> error("Invalid role: $this")
        }

        /** Convert from string representing Gemini role to [TextChatRole]. */
        fun String?.fromGeminiRole() = when (this) {
            GEMINI_ROLE_USER -> TextChatRole.User
            GEMINI_ROLE_MODEL -> TextChatRole.Assistant
            else -> error("Invalid role: $this")
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
    val description: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val supportedGenerationMethods: List<String>,
    val temperature: Double? = null,
    val maxTemperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null
)

@Serializable
data class BatchEmbedContentRequest(
    val requests: List<EmbedContentRequest>
)

@Serializable
data class BatchEmbedContentsResponse(
    val embeddings: List<ContentEmbedding>
)

@Serializable
data class EmbedContentRequest(
    val content: Content,
    val model: String? = null,
    val title: String? = null,
    val outputDimensionality: Int? = null
)

@Serializable
data class EmbedContentResponse(
    val embedding: ContentEmbedding
)

@Serializable
data class ContentEmbedding(
    val values: List<Float>
)

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
//    val safetySettings: SafetySetting? = null,
    val systemInstruction: Content? = null, // this is a beta feature
    val generationConfig: GenerationConfig? = null,
) {
    constructor(content: Content, systemInstruction: Content? = null, generationConfig: GenerationConfig? = null) :
            this(listOf(content), systemInstruction, generationConfig)
}

private const val GEMINI_ROLE_USER = "user"
private const val GEMINI_ROLE_MODEL = "model"

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
) {
    init { require(role in listOf(null, GEMINI_ROLE_USER, GEMINI_ROLE_MODEL)) { "Invalid role: $role" } }

    companion object {
        /** Content with a single text part. */
        fun text(text: String) = Content(listOf(Part(text)), GEMINI_ROLE_USER)
        /** Content with a system message. */
        fun systemMessage(text: String) = text(text) // TODO - support for system messages if Gemini supports it
    }
}

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: Blob? = null
)

@Serializable
data class Blob(
    val mimeType: String,
    val data: String
) {
    companion object {
        /** Generate blob from image URL. */
        fun image(url: URI) = image(url.toASCIIString())

        /** Generate blob from image URL. */
        fun image(urlStr: String): Blob {
            if (urlStr.startsWith("data:image/")) {
                val mimeType = urlStr.substringBefore(";base64,").substringAfter("data:")
                val base64 = urlStr.substringAfter(";base64,")
                return Blob(mimeType, base64)
            } else {
                throw UnsupportedOperationException("Only data URLs are supported for images.")
            }
        }
    }
}

@Serializable
data class GenerationConfig(
    val stopSequences: List<String>? = null,
    val responseMimeType: String? = null,
    val candidateCount: Int? = null,
    val maxOutputTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null
) {
    init {
        require(responseMimeType in listOf(null, "text/plain", "application/json")) { "Invalid responseMimeType: $responseMimeType" }
    }
}

@Serializable
data class GenerateContentResponse(
    var error: Error? = null,
    var candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content
)

@Serializable
data class Error(
    val message: String
)

//endregion
