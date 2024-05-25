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
package tri.ai.openai

import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.core.Usage
import com.aallam.openai.api.edits.EditsRequest
import com.aallam.openai.api.embedding.EmbeddingRequest
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.*
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import tri.ai.openai.OpenAiModelIndex.AUDIO_WHISPER
import tri.ai.openai.OpenAiModelIndex.EMBEDDING_ADA
import tri.ai.openai.OpenAiModelIndex.IMAGE_DALLE2
import tri.ai.pips.AiTaskResult
import tri.ai.pips.AiTaskResult.Companion.result
import tri.ai.pips.UsageUnit
import java.io.File
import java.time.Duration
import java.util.*
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

/** OpenAI API client with built-in usage tracking. */
class OpenAiClient(val settings: OpenAiSettings) {

    /** OpenAI API client. */
    val client
        get() = settings.client
    /** OpenAI API usage stats. */
    val usage = mutableMapOf<UsageUnit, Int>()

    //region QUICK API CALLS

    /** Checks for an OpenAI API key, if the base URL points to OpenAI. */
    private fun checkApiKey() {
        val isOpenAi = settings.baseUrl.let { it == null || it.contains("api.openai.com") }
        val isValidKey = isOpenAi && settings.apiKey.startsWith("sk-") && !settings.apiKey.trim().contains(" ")
        if (!isValidKey)
            throw UnsupportedOperationException("Invalid API key. Please set a valid OpenAI API key.")
    }

    /** Runs an embedding using ADA embedding model. */
    suspend fun quickEmbedding(modelId: String = EMBEDDING_ADA, outputDimensionality: Int? = null, inputs: List<String>): AiTaskResult<List<List<Double>>> {
        checkApiKey()
        return quickEmbedding(modelId, outputDimensionality, *inputs.toTypedArray())
    }

    /** Runs an embedding using ADA embedding model. */
    suspend fun quickEmbedding(modelId: String, outputDimensionality: Int? = null, vararg inputs: String): AiTaskResult<List<List<Double>>> {
        checkApiKey()
        return client.embeddings(EmbeddingRequest(
            ModelId(modelId),
            inputs.toList(),
            dimensions = outputDimensionality
        )).let { it ->
            usage.increment(it.usage)
            result(it.embeddings.map { it.embedding }, modelId)
        }
    }

    /** Runs a quick audio transcription for a given file. */
    suspend fun quickTranscribe(modelId: String = AUDIO_WHISPER, audioFile: File): AiTaskResult<String> {
        checkApiKey()
        if (!audioFile.isAudioFile())
            return AiTaskResult.invalidRequest("Audio file not provided.")

        val request = TranscriptionRequest(
            model = ModelId(modelId),
            audio = FileSource(audioFile.toOkioPath(), FileSystem.SYSTEM)
        )
        return client.transcription(request).let {
            usage.increment(0, UsageUnit.AUDIO_MINUTES)
            result(it.text, modelId)
        }
    }

    //endregion

    //region DIRECT API CALLS

    /** Runs a text completion request. */
    suspend fun completion(completionRequest: CompletionRequest): AiTaskResult<String> {
        checkApiKey()
        val t0 = System.currentTimeMillis()
        val resp = client.completion(completionRequest)
        usage.increment(resp.usage)
        val millis = Duration.ofMillis(System.currentTimeMillis() - t0)
        return AiTaskResult(
            value = resp.choices[0].text,
            modelId = completionRequest.model.id,
            duration = millis,
            durationTotal = millis
        )
    }

    /** Runs a text completion request using a chat model. */
    suspend fun chatCompletion(completionRequest: ChatCompletionRequest): AiTaskResult<String> {
        checkApiKey()
        val t0 = System.currentTimeMillis()
        val resp = client.chatCompletion(completionRequest)
        val millis = Duration.ofMillis(System.currentTimeMillis() - t0)
        return AiTaskResult(
            value = resp.choices[0].message.content ?: "",
            modelId = completionRequest.model.id,
            duration = millis,
            durationTotal = millis
        )
    }

    /** Runs a chat response. */
    suspend fun chat(completionRequest: ChatCompletionRequest): AiTaskResult<ChatMessage> {
        checkApiKey()
        return client.chatCompletion(completionRequest).let {
            usage.increment(it.usage)
            result(it.choices[0].message, completionRequest.model.id)
        }
    }

    /** Runs an edit request (deprecated API). */
    @Suppress("DEPRECATION")
    suspend fun edit(request: EditsRequest): AiTaskResult<String> {
        checkApiKey()
        return client.edit(request).let {
            usage.increment(it.usage)
            result(it.choices[0].text, request.model.id)
        }
    }

    /** Runs an image creation request. */
    suspend fun imageURL(imageCreation: ImageCreation): AiTaskResult<List<String>> {
        checkApiKey()
        return client.imageURL(imageCreation).let {
            usage.increment(it.size, UsageUnit.IMAGES)
            result(it.map { it.url }, IMAGE_DALLE2)
        }
    }

    //endregion

    //region USAGE TRACKING

    /** Increment usage map with usage from response. */
    private fun MutableMap<UsageUnit, Int>.increment(usage: Usage?) {
        this[UsageUnit.TOKENS] = (this[UsageUnit.TOKENS] ?: 0) + (usage?.totalTokens ?: 0)
    }

    /** Increment usage map with usage from response. */
    private fun MutableMap<UsageUnit, Int>.increment(totalTokens: Int, unit: UsageUnit) {
        this[unit] = (this[unit] ?: 0) + totalTokens
    }

    //endregion

    companion object {
        val INSTANCE by lazy { OpenAiClient(OpenAiSettings()) }
    }

}

/** Manages OpenAI API key and client. */
class OpenAiSettings {

    companion object {
        const val API_KEY_FILE = "apikey.txt"
        const val API_KEY_ENV = "OPENAI_API_KEY"
    }

    var baseUrl: String? = null
        set(value) {
            field = value
            buildClient()
        }

    var apiKey = readApiKey()
        set(value) {
            field = value
            buildClient()
        }

    var logLevel = LogLevel.Info
        set(value) {
            field = value
            buildClient()
        }

    var timeoutSeconds = 60
        set(value) {
            field = value
            buildClient()
        }

    var client: OpenAI
        private set

    init {
        client = buildClient()
    }

    /** Read API key by first checking for [API_KEY_FILE], and then checking user environment variable [API_KEY_ENV]. */
    private fun readApiKey(): String {
        val file = File(API_KEY_FILE)

        val key = if (file.exists()) {
            file.readText()
        } else
            System.getenv(API_KEY_ENV)

        return if (key.isNullOrBlank()) {
            Logger.getLogger(OpenAiSettings::class.java.name).warning(
                "No API key found. Please create a file named $API_KEY_FILE in the root directory, or set an environment variable named $API_KEY_ENV."
            )
            ""
        } else
            key
    }

    @Throws(IllegalStateException::class)
    private fun buildClient(): OpenAI {
        client = OpenAI(
            OpenAIConfig(
                host = if (baseUrl == null) OpenAIHost.OpenAI else OpenAIHost(baseUrl!!),
                token = apiKey,
                logging = LoggingConfig(LogLevel.None),
                timeout = Timeout(socket = timeoutSeconds.seconds)
            )
        )
        return client
    }

}

//region UTILS

val jsonMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)!!
val yamlMapper = ObjectMapper(YAMLFactory())
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)!!

val jsonWriter: ObjectWriter = jsonMapper.writerWithDefaultPrettyPrinter()
val yamlWriter: ObjectWriter = yamlMapper.writerWithDefaultPrettyPrinter()

fun File.isAudioFile() = extension.lowercase(Locale.getDefault()) in
        listOf("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm")

//endregion
