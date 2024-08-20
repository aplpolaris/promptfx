/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.core.VisionLanguageChatMessage
import java.io.Closeable
import java.io.File
import java.net.URI
import java.util.logging.Logger

/** General purpose client for the Gemini API. */
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
            generationConfig = numResponses?.let { GenerationConfig(candidateCount = it) }
        ))

    suspend fun generateContent(prompt: String, image: String, modelId: String, numResponses: Int? = null): GenerateContentResponse {
        val request = GenerateContentRequest(
            content = Content(listOf(
                Part(text = prompt),
                Part(inlineData = Blob(image, "image/jpeg"))
            )),
            generationConfig = numResponses?.let { GenerationConfig(candidateCount = it) }
        )
        return generateContent(modelId, request)
    }

    suspend fun generateContent(messages: List<TextChatMessage>, modelId: String, config: GenerationConfig? = null): GenerateContentResponse {
        val system = messages.lastOrNull { it.role == TextChatRole.System }?.content
        val request = GenerateContentRequest(
            messages.filter { it.role != TextChatRole.System }.map {
                val role = when (it.role) {
                    TextChatRole.User -> "user"
                    TextChatRole.Assistant -> "model"
                    else -> error("Invalid role: ${it.role}")
                }
                Content(listOf(Part(it.content)), role)
            },
            systemInstruction = system?.let { Content(listOf(Part(it)), "system") },
            generationConfig = config
        )
        return generateContent(modelId, request)
    }

    suspend fun generateContentVision(messages: List<VisionLanguageChatMessage>, modelId: String, config: GenerationConfig? = null): GenerateContentResponse {
        val system = messages.lastOrNull { it.role == TextChatRole.System }?.content
        val request = GenerateContentRequest(
            messages.filter { it.role != TextChatRole.System }.map {
                val role = when (it.role) {
                    TextChatRole.User -> "user"
                    TextChatRole.Assistant -> "model"
                    else -> error("Invalid role: ${it.role}")
                }
                Content(listOf(
                    Part(it.content),
                    Part(null, Blob.image(it.image))
                ), role)
            },
            systemInstruction = system?.let { Content(listOf(Part(it)), "system") },
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
    }

}

//region API SETTINGS

/** Manages Gemini API key and client. */
@OptIn(ExperimentalSerializationApi::class)
class GeminiSettings {

    companion object {
        const val API_KEY_FILE = "apikey-gemini.txt"
        const val API_KEY_ENV = "GEMINI_API_KEY"
        const val BASE_URL = " https://generativelanguage.googleapis.com/v1beta/"
    }

    var baseUrl = BASE_URL
        set(value) {
            field = value
            buildClient()
        }

    var apiKey = readApiKey()
        set(value) {
            field = value
            buildClient()
        }

    var timeoutSeconds = 60
        set(value) {
            field = value
            buildClient()
        }

    /** The HTTP client used to make requests. */
    var client: HttpClient = buildClient()

    /** Read API key by first checking for [API_KEY_FILE], and then checking user environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)

        val key = if (file.exists()) {
            file.readText()
        } else
            System.getenv(API_KEY_ENV)

        return if (key.isNullOrBlank()) {
            Logger.getLogger(GeminiSettings::class.java.name).warning(
                "No API key found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV."
            )
            ""
        } else
            key
    }

    @Throws(IllegalStateException::class)
    private fun buildClient() = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        install(Logging) {
            logger = io.ktor.client.plugins.logging.Logger.SIMPLE
            level = LogLevel.NONE
        }
        install(HttpTimeout) {
            socketTimeoutMillis = timeoutSeconds * 1000L
            connectTimeoutMillis = timeoutSeconds * 1000L
            requestTimeoutMillis = timeoutSeconds * 1000L
        }
        defaultRequest {
            url(baseUrl)
            url.parameters.append("key", apiKey)
            contentType(ContentType.Application.Json)
        }
    }.also { client = it }

}

//endregion

//region DTO's

@Serializable
data class ModelsResponse(
    val models: List<ModelInfo>
)

@Serializable
data class ModelInfo(
    val name: String,
    val baseModelId: String? = null,
    val version: String,
    val displayName: String,
    val description: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val supportedGenerationMethods: List<String>,
    val temperature: Double? = null,
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

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
) {
    init { require(role in listOf(null, "user", "model")) { "Invalid role: $role" } }

    companion object {
        /** Content with a single text part. */
        fun text(text: String) = Content(listOf(Part(text)), "user")
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