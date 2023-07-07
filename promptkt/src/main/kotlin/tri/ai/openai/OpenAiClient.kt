package tri.ai.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.core.Usage
import com.aallam.openai.api.edits.EditsRequest
import com.aallam.openai.api.embedding.EmbeddingRequest
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.*
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import tri.ai.pips.AiTaskResult
import tri.ai.pips.AiTaskResult.Companion.result
import tri.ai.pips.UsageUnit
import java.io.File
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

    /** Runs an embedding using ADA embedding model. */
    suspend fun quickEmbedding(modelId: String = EMBEDDING_ADA, inputs: List<String>) =
        quickEmbedding(modelId, *inputs.toTypedArray())

    /** Runs an embedding using ADA embedding model. */
    suspend fun quickEmbedding(modelId: String, vararg inputs: String) = client.embeddings(EmbeddingRequest(
        ModelId(modelId),
        inputs.toList()
    )).let { it ->
        usage.increment(it.usage)
        result(it.embeddings.map { it.embedding }, modelId)
    }

    /** Runs a quick audio transcription for a given file. */
    @OptIn(BetaOpenAI::class)
    suspend fun quickTranscribe(modelId: String = AUDIO_WHISPER, audioFile: File): AiTaskResult<String> {
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
    suspend fun completion(completionRequest: CompletionRequest) =
        client.completion(completionRequest).let {
            usage.increment(it.usage)
            result(it.choices[0].text, completionRequest.model.id)
        }

    /** Runs a text completion request using a chat model. */
    @OptIn(BetaOpenAI::class)
    suspend fun chatCompletion(completionRequest: ChatCompletionRequest) =
        client.chatCompletion(completionRequest).let {
            usage.increment(it.usage)
            result(it.choices[0].message!!.content!!, completionRequest.model.id)
        }

    /** Runs an edit request. */
    suspend fun edit(request: EditsRequest) =
        client.edit(request).let {
            usage.increment(it.usage)
            result(it.choices[0].text, request.model.id)
        }

    /** Runs an image creation request. */
    @OptIn(BetaOpenAI::class)
    suspend fun imageURL(imageCreation: ImageCreation) =
        client.imageURL(imageCreation).let {
            usage.increment(it.size, UsageUnit.IMAGES)
            result(it.map { it.url }, IMAGE_DALLE)
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

    val API_KEY_FILE = "apikey.txt"
    val API_KEY_ENV = "OPENAI_API_KEY"

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
        val file = File("apikey.txt")

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
        if (baseUrl.isNullOrBlank() && apiKey.isBlank())
            throw IllegalStateException("Missing API key")
        client = OpenAI(
            OpenAIConfig(
                host = if (baseUrl == null) OpenAIHost.OpenAI else OpenAIHost(baseUrl!!),
                token = apiKey ?: "",
                logLevel = LogLevel.None,
                timeout = Timeout(socket = timeoutSeconds.seconds)
            )
        )
        return client
    }

}

//region MODELS

val COMBO_GPT4 = "gpt-4"
val COMBO_GPT35 = "gpt-3.5-turbo"
val COMBO_GPT35_16K = "gpt-3.5-turbo-16k"

val TEXT_DAVINCI3 = "text-davinci-003"
val TEXT_DAVINCI2 = "text-davinci-002"
val TEXT_CURIE = "text-curie-001"
val TEXT_BABBAGE = "text-babbage-001"
val TEXT_ADA = "text-ada-001"

val EDIT_DAVINCI = "text-davinci-edit-001"

val INSERT_DAVINCI2 = "text-davinci-insert-002"
val INSERT_DAVINCI = "text-davinci-insert-001"

val CODE_DAVINCI2 = "code-davinci-002"
val CODE_CUSHMAN1 = "code-cushman-001"
val CODE_EDIT_DAVINCI = "code-davinci-edit-001"

val EMBEDDING_ADA = "text-embedding-ada-002"

val AUDIO_WHISPER = "whisper-1"
val IMAGE_DALLE = "dalle-2"

val chatModels = listOf(COMBO_GPT35, COMBO_GPT4, "$COMBO_GPT35-0301", "$COMBO_GPT4-0314")
val textModels = listOf(TEXT_DAVINCI3, TEXT_CURIE, TEXT_BABBAGE, TEXT_ADA)
val codeModels = listOf(CODE_DAVINCI2, CODE_CUSHMAN1)
val completionModels = textModels + codeModels
val insertModels = listOf(TEXT_DAVINCI3, INSERT_DAVINCI2, INSERT_DAVINCI, TEXT_DAVINCI2, CODE_DAVINCI2)
val editsModels = listOf(EDIT_DAVINCI, CODE_EDIT_DAVINCI)
val embeddingsModels = listOf(EMBEDDING_ADA)
val audioModels = listOf(AUDIO_WHISPER)
val imageModels = listOf(IMAGE_DALLE)

//endregion

//region UTILS

val mapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)!!

fun File.isAudioFile() = extension.toLowerCase() in listOf("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm")

//endregion
