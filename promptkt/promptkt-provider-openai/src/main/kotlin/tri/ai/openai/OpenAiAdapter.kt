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
package tri.ai.openai

import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.core.Usage
import com.aallam.openai.api.edits.EditsRequest
import com.aallam.openai.api.embedding.EmbeddingRequest
import com.aallam.openai.api.file.File as OpenAiFile
import com.aallam.openai.api.file.FileId
import com.aallam.openai.api.file.Purpose
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.file.fileUpload
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.response.ResponseRequest
import com.aallam.openai.client.OpenAI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tri.ai.core.*
import tri.ai.openai.OpenAiModelIndex.AUDIO_WHISPER
import tri.ai.openai.OpenAiModelIndex.DALLE2_ID
import tri.ai.openai.OpenAiModelIndex.EMBEDDING_ADA
import tri.ai.openai.api.OpenAiApiSettings
import tri.ai.prompt.PromptTemplate.Companion.INPUT
import tri.ai.prompt.PromptTemplate.Companion.INSTRUCT
import tri.ai.prompt.trace.*
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.*

/** Adapter for an OpenAI client with usage tracking. */
class OpenAiAdapter(val settings: OpenAiApiSettings, _client: OpenAI) {

    var client = _client
        internal set

    /** Ktor HTTP client for direct API calls (used for models that don't support response_format). */
    private val directHttpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
        }
    }

    companion object {
        private val INSTANCE_SETTINGS = OpenAiApiSettingsBasic()
        val INSTANCE = OpenAiAdapter(INSTANCE_SETTINGS, INSTANCE_SETTINGS.buildClient())
        var apiKey
            get() = INSTANCE_SETTINGS.apiKey
            set(value) {
                INSTANCE_SETTINGS.apiKey = value
                INSTANCE.client = INSTANCE_SETTINGS.buildClient()
            }
    }

    /** OpenAI API usage stats. */
    val usage = mutableMapOf<UsageUnit, Int>()

    //region QUICK API CALLS

    /** Runs an embedding using ADA embedding model. */
    suspend fun quickEmbedding(modelId: String = EMBEDDING_ADA, outputDimensionality: Int? = null, inputs: List<String>): AiPromptTrace {
        settings.checkApiKey()
        return quickEmbedding(modelId, outputDimensionality, *inputs.toTypedArray())
    }

    /**
     * Runs an embedding using ADA embedding model.
     * Generates one [AiOutput] object per input string.
     */
    suspend fun quickEmbedding(modelId: String, outputDimensionality: Int? = null, vararg inputs: String): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.embeddings(EmbeddingRequest(
            ModelId(modelId),
            inputs.toList(),
            dimensions = outputDimensionality
        ))
        usage.increment(resp.usage)

        return AiTaskTrace(
            env = AiEnvInfo.of(AiModelInfo.embedding(modelId, outputDimensionality)),
            exec = AiExecInfo.durationSince(t0, queryTokens = resp.usage.promptTokens, responseTokens = resp.usage.completionTokens),
            output = AiOutputInfo.output(resp.embeddings.map { AiOutput.Other(it.embedding) })
        )
    }

    /** Runs a quick audio transcription for a given file. */
    suspend fun quickTranscribe(modelId: String = AUDIO_WHISPER, audioFile: File): AiPromptTrace {
        settings.checkApiKey()
        if (!audioFile.isAudioFile())
            return AiTaskTrace.invalidRequest(modelId, "Audio file not provided.")

        val t0 = System.currentTimeMillis()
        val resp = client.transcription(TranscriptionRequest(
            model = ModelId(modelId),
            audio = FileSource(Path(audioFile.absolutePath), SystemFileSystem)
        ))
        resp.duration?.let {
            usage.increment(it.toInt(), UsageUnit.AUDIO_SECONDS)
        }

        return AiTaskTrace(
            env = AiEnvInfo.of(AiModelInfo(modelId)),
            exec = AiExecInfo.durationSince(t0),
            output = AiOutputInfo.text(resp.text)
        )
    }

    //endregion

    //region DIRECT API CALLS

    /** Runs a text completion request. */
    suspend fun completion(completionRequest: CompletionRequest): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.completion(completionRequest)
        usage.increment(resp.usage)

        return AiTaskTrace(
            env = AiEnvInfo.of(completionRequest.toModelInfo()),
            input = completionRequest.prompt?.let { AiTaskInputInfo.of(PromptInfo(it)) },
            exec = AiExecInfo.durationSince(t0, queryTokens = resp.usage?.promptTokens, responseTokens = resp.usage?.completionTokens),
            output = AiOutputInfo.text(resp.choices.map { it.text })
        )
    }

    suspend fun chatCompletion(completionRequest: ChatCompletionRequest) =
        chat(completionRequest, multimodal = false).mapOutput { AiOutput.Text(it.textContent(ifNone = "(no response)")) }

    /**
     * Runs a chat response.
     * If [multimodal] is true, converts to a [MultimodalChatMessage], otherwise to a [TextChatMessage].
     */
    suspend fun chat(completionRequest: ChatCompletionRequest, multimodal: Boolean): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val userInputOnly = completionRequest.messages.size == 1 && completionRequest.messages.first().role == ChatRole.User
        val prompt = if (!userInputOnly) null else
            completionRequest.messages.first().messageContent?.let {
                when {
                    it is TextContent -> it.content
                    it is ListContent && it.content.size == 1 && it.content[0] is TextPart -> (it.content[0] as TextPart).text
                    else -> null
                }
            }
        val resp = client.chatCompletion(completionRequest)
        usage.increment(resp.usage)

        val outputInfo = when {
            multimodal -> AiOutputInfo.multimodalMessages(resp.choices.map { it.message.fromOpenAiMessageToMultimodal() })
            else -> AiOutputInfo.messages(resp.choices.map { it.message.fromOpenAiMessage() })
        }

        return AiTaskTrace(
            env = AiEnvInfo.of(completionRequest.toModelInfo()),
            input = prompt?.let { AiTaskInputInfo.of(PromptInfo(it)) },
            exec = AiExecInfo.durationSince(t0, queryTokens = resp.usage?.promptTokens, responseTokens = resp.usage?.completionTokens),
            output = outputInfo
        )
    }

    /** Runs a request using the OpenAI Responses API. */
    suspend fun responseCompletion(request: ResponseRequest): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.response(request)
        resp.usage?.let { usage[UsageUnit.TOKENS] = (usage[UsageUnit.TOKENS] ?: 0) + (it.totalTokens ?: 0) }

        val messageItems = resp.output.filter { it.type == "message" }
        val functionCallItems = resp.output.filter { it.type == "function_call" }

        var outputMessages = messageItems.map { item ->
            MultimodalChatMessage(
                role = MChatRole.Assistant,
                content = item.content?.mapNotNull { contentPart ->
                    contentPart.text?.let { text -> MChatMessagePart(text = text) }
                } ?: emptyList()
            )
        } + if (functionCallItems.isNotEmpty()) {
            listOf(MultimodalChatMessage(
                role = MChatRole.Assistant,
                toolCalls = functionCallItems.map { item ->
                    MToolCall(
                        id = item.callId ?: item.id ?: "",
                        name = item.name ?: "",
                        argumentsAsJson = item.arguments ?: ""
                    )
                }
            ))
        } else {
            emptyList()
        }

        // Fallback to outputText if no structured output items were returned (some models/configs omit `output`)
        val fallbackText = resp.outputText
        if (outputMessages.isEmpty() && fallbackText != null) {
            outputMessages = listOf(MultimodalChatMessage(
                role = MChatRole.Assistant,
                content = listOf(MChatMessagePart(text = fallbackText))
            ))
        }

        val modelInfo = AiModelInfo.info(request.model.id,
            AiModelInfo.MAX_TOKENS to request.maxOutputTokens,
            AiModelInfo.TEMPERATURE to request.temperature,
            AiModelInfo.TOP_P to request.topP
        )
        val execInfo = AiExecInfo.durationSince(t0, queryTokens = resp.usage?.inputTokens, responseTokens = resp.usage?.outputTokens)

        // Propagate API-level errors (status:"failed") as a failed trace rather than returning empty output
        if (resp.status == "failed") {
            val errorMsg = resp.error?.message ?: "Responses API returned status: failed for model ${request.model.id}"
            return AiTaskTrace(env = AiEnvInfo.of(modelInfo), exec = execInfo.also { it.error = errorMsg })
        }

        return AiTaskTrace(env = AiEnvInfo.of(modelInfo), exec = execInfo, output = AiOutputInfo.multimodalMessages(outputMessages))
    }

    /** Runs an edit request (deprecated API). */
    @Suppress("DEPRECATION")
    suspend fun edit(request: EditsRequest): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.edit(request)
        usage.increment(resp.usage)

        return AiTaskTrace(
            env = AiEnvInfo.of(request.toModelInfo()),
            input = AiTaskInputInfo.of(request.toPromptInfo()),
            exec = AiExecInfo.durationSince(t0, queryTokens = resp.usage.promptTokens, responseTokens = resp.usage.completionTokens),
            output = AiOutputInfo.text(resp.choices.map { it.text })
        )
    }

    /** Runs an image creation request. */
    suspend fun imageURL(imageCreation: ImageCreation): AiTaskTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.imageURL(imageCreation)
        usage.increment(resp.size, UsageUnit.IMAGES)

        return AiTaskTrace(
            env = AiEnvInfo.of(imageCreation.toModelInfo()),
            exec = AiExecInfo.durationSince(t0),
            output = AiOutputInfo.multimodalMessages(resp.map { MultimodalChatMessage.imageUrl(imageUrl = it.url) })
        )
    }

    /** Runs an image creation request. */
    suspend fun imageJSON(imageCreation: ImageCreation): AiTaskTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.imageJSON(imageCreation)
        usage.increment(resp.size, UsageUnit.IMAGES)

        return AiTaskTrace(
            env = AiEnvInfo.of(imageCreation.toModelInfo()),
            exec = AiExecInfo.durationSince(t0),
            output = AiOutputInfo.multimodalMessages(resp.map { MultimodalChatMessage.imageBase64(imageBase64 = it.b64JSON) })
        )
    }

    /**
     * Runs an image creation request without the response_format parameter.
     * Required for models such as gpt-image-1 that do not support response_format.
     */
    suspend fun imageJSONDirect(imageCreation: ImageCreation): AiTaskTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val request = DirectImageRequest(
            model = imageCreation.model?.id ?: DALLE2_ID,
            prompt = imageCreation.prompt,
            n = imageCreation.n,
            size = imageCreation.size?.size,
            quality = imageCreation.quality?.value,
            style = imageCreation.style?.value
        )
        val baseUrl = (settings.baseUrl ?: "https://api.openai.com/v1").trimEnd('/') + "/"
        val resp = directHttpClient.post("${baseUrl}images/generations") {
            header("Authorization", "Bearer ${settings.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<DirectImageResponse>()
        usage.increment(resp.data.size, UsageUnit.IMAGES)

        return AiTaskTrace(
            env = AiEnvInfo.of(imageCreation.toModelInfo()),
            exec = AiExecInfo.durationSince(t0),
            output = AiOutputInfo.multimodalMessages(resp.data.mapNotNull { it.b64Json }.map { MultimodalChatMessage.imageBase64(imageBase64 = it) })
        )
    }

    /** Runs a speech request. */
    suspend fun speech(request: SpeechRequest): AiPromptTraceSupport {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.speech(request)

        return AiTaskTrace(
            env = AiEnvInfo.of(request.toModelInfo()),
            exec = AiExecInfo.durationSince(t0),
            output = AiOutputInfo.other(resp)
        )
    }

    //endregion

    //region FILES API

    /** Uploads a file to the OpenAI Files API. */
    suspend fun uploadFile(source: FileSource, purpose: String): OpenAiFile {
        settings.checkApiKey()
        return client.file(fileUpload {
            file = source
            this.purpose = Purpose(purpose)
        })
    }

    /** Lists all files uploaded to the OpenAI Files API. */
    suspend fun listFiles(): List<OpenAiFile> {
        settings.checkApiKey()
        return client.files()
    }

    /** Retrieves metadata for a specific file by its ID. */
    suspend fun getFile(fileId: String): OpenAiFile? {
        settings.checkApiKey()
        return client.file(FileId(fileId))
    }

    /** Deletes a file from the OpenAI Files API. Returns true if deleted successfully. */
    suspend fun deleteFile(fileId: String): Boolean {
        settings.checkApiKey()
        return client.delete(FileId(fileId))
    }

    /** Downloads the content of a file from the OpenAI Files API. */
    suspend fun downloadFile(fileId: String): ByteArray {
        settings.checkApiKey()
        return client.download(FileId(fileId))
    }

    //endregion

    //region PARAMETER CONVERSIONS

    private fun CompletionRequest.toModelInfo() = AiModelInfo.info(model.id,
        AiModelInfo.MAX_TOKENS to maxTokens,
        AiModelInfo.TEMPERATURE to temperature,
        AiModelInfo.TOP_P to topP,
        AiModelInfo.NUM_RESPONSES to n,
        AiModelInfo.LOG_PROBS to logprobs,
        AiModelInfo.ECHO to echo,
        AiModelInfo.STOP to stop,
        AiModelInfo.PRESENCE_PENALTY to presencePenalty,
        AiModelInfo.FREQUENCY_PENALTY to frequencyPenalty,
        AiModelInfo.BEST_OF to bestOf,
        AiModelInfo.LOGIT_BIAS to logitBias,
        AiModelInfo.USER to user,
        AiModelInfo.SUFFIX to suffix
    )

    private fun ChatCompletionRequest.toModelInfo() = AiModelInfo.info(model.id,
        AiModelInfo.MAX_TOKENS to maxTokens,
        AiModelInfo.TEMPERATURE to temperature,
        AiModelInfo.TOP_P to topP,
        AiModelInfo.NUM_RESPONSES to n,
        AiModelInfo.LOG_PROBS to logprobs,
        AiModelInfo.TOP_LOG_PROBS to topLogprobs,
        AiModelInfo.STOP to stop,
        AiModelInfo.PRESENCE_PENALTY to presencePenalty,
        AiModelInfo.FREQUENCY_PENALTY to frequencyPenalty,
        AiModelInfo.LOGIT_BIAS to logitBias,
        AiModelInfo.USER to user,
// TODO - what is appropriate serialization within parameter map?
//        AiModelInfo.FUNCTIONS to functions.map { it.toString() },
//        AiModelInfo.FUNCTION_CALL to functionCall?.toString(),
//        AiModelInfo.TOOLS to tools?.map { it.toString() },
//        AiModelInfo.TOOL_CHOICE to toolChoice?.toString(),
        AiModelInfo.RESPONSE_FORMAT to responseFormat?.type,
// TODO - seed is beta
//        AiModelInfo.SEED to seed
    )

    private fun EditsRequest.toModelInfo() = AiModelInfo.info(model.id,
        AiModelInfo.TEMPERATURE to temperature,
        AiModelInfo.TOP_P to topP
    )

    private fun EditsRequest.toPromptInfo() = PromptInfo(instruction,
        INPUT to input,
        INSTRUCT to instruction
    )

    private fun ImageCreation.toModelInfo() = AiModelInfo.info(model?.id ?: DALLE2_ID,
        AiModelInfo.NUM_RESPONSES to n,
        AiModelInfo.SIZE to size?.size,
        AiModelInfo.USER to user,
        AiModelInfo.QUALITY to quality?.value,
        AiModelInfo.STYLE to style?.value
    )

    private fun SpeechRequest.toModelInfo() = AiModelInfo.info(model.id,
        AiModelInfo.VOICE to voice?.value,
        AiModelInfo.RESPONSE_FORMAT to responseFormat?.value,
        AiModelInfo.SPEED to speed
    )

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

}

//region UTILS

/** Tracks model usage. */
enum class UsageUnit {
    TOKENS,
    AUDIO_SECONDS,
    IMAGES,
    NONE
}

fun File.isAudioFile() = extension.lowercase(Locale.getDefault()) in
        listOf("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm")

/** Request body for image generation without response_format. */
@Serializable
private data class DirectImageRequest(
    val model: String,
    val prompt: String,
    val n: Int? = null,
    val size: String? = null,
    val quality: String? = null,
    val style: String? = null
)

/** Response from image generation API. */
@Serializable
private data class DirectImageResponse(
    val created: Long = 0,
    val data: List<DirectImageData> = emptyList()
)

/** A single image item in the image generation response. */
@Serializable
private data class DirectImageData(
    @SerialName("b64_json") val b64Json: String? = null
)

//endregion

//region OpenAI CONVERSIONS

/** Convert from [MChatRole] to OpenAI [ChatRole]. */
fun MChatRole.toOpenAiRole() = when (this) {
    MChatRole.System -> ChatRole.System
    MChatRole.User -> ChatRole.User
    MChatRole.Assistant -> ChatRole.Assistant
    else -> error("Invalid role: $this")
}

/** Convert from OpenAI [ChatRole] to [MChatRole]. */
fun ChatRole.fromOpenAiRole() = when (this) {
    ChatRole.System -> MChatRole.System
    ChatRole.User -> MChatRole.User
    ChatRole.Assistant -> MChatRole.Assistant
    else -> error("Invalid role: $this")
}

/** Convert from [TextChatMessage] to OpenAI [ChatMessage]. */
fun TextChatMessage.toOpenAiMessage() =
    ChatMessage(role.toOpenAiRole(), content)

/** Convert from OpenAI [ChatMessage] to [TextChatMessage]. */
fun ChatMessage.fromOpenAiMessage(): TextChatMessage {
    val mc = messageContent
    val usePrompt = when {
        mc is TextContent -> mc.content
        mc is ListContent && mc.content.size == 1 && mc.content[0] is TextPart -> (mc.content[0] as TextPart).text
        else -> null
    }
    return TextChatMessage(role.fromOpenAiRole(), usePrompt)
}

/** Convert from OpenAI [ChatMessage] to [MultimodalChatMessage]. */
fun ChatMessage.fromOpenAiMessageToMultimodal() = MultimodalChatMessage(
    role = role.fromOpenAiRole(),
    content = messageContent?.fromOpenAiContent(),
    toolCalls = toolCalls?.map { (it as ToolCall.Function).fromOpenAi() },
    toolCallId = toolCallId?.id
)

private fun Content.fromOpenAiContent(): List<MChatMessagePart> {
    return when (this) {
        is TextContent -> listOf(MChatMessagePart(text = content))
        is ListContent -> content.map {
            MChatMessagePart(
                partType = if (it is TextPart) MPartType.TEXT else if (it is ImagePart) MPartType.IMAGE else throw IllegalStateException(),
                text = (it as? TextPart)?.text,
                inlineData = (it as? ImagePart)?.imageUrl?.url
            )
        }
    }
}

private fun ToolCall.Function.fromOpenAi() = MToolCall(
    id = id.id,
    name = function.name,
    argumentsAsJson = function.arguments
)

/** Convert from OpenAI [Model] to [ModelInfo]. */
fun Model.toModelInfo(source: String): ModelInfo {
    val existing = OpenAiModelIndex.modelInfoIndex[id.id]
    val info = existing ?: ModelInfo(id.id, ModelType.UNKNOWN, source)
    created?.let {
        info.metadata.created = Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    if (info.type == ModelType.UNKNOWN) {
        when {
            "moderation" in id.id -> info.type = ModelType.MODERATION
            "-realtime-" in id.id -> {
                info.type = ModelType.REALTIME_CHAT
                info.capabilities.inputs = listOf(DataModality.text, DataModality.audio)
                info.capabilities.outputs = listOf(DataModality.text, DataModality.audio)
            }
            "-audio-" in id.id -> {
                info.type = ModelType.AUDIO_CHAT
                info.capabilities.inputs = listOf(DataModality.audio)
                info.capabilities.outputs = listOf(DataModality.audio)
            }
            else -> {
                // attempt to assign type for tagged models based on a "parent type"
                var possibleTypeId = id.id
                while (OpenAiModelIndex.modelInfoIndex[possibleTypeId]?.type in listOf(null, ModelType.UNKNOWN)) {
                    possibleTypeId = possibleTypeId.substringBeforeLast("-")
                    if ("-" !in possibleTypeId) break
                }
                info.type = OpenAiModelIndex.modelInfoIndex[possibleTypeId]?.type ?: ModelType.UNKNOWN
            }
        }
    }

    return info
}

//endregion
