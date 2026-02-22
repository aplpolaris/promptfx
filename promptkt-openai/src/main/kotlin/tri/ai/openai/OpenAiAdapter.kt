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
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import okio.FileSystem
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

        return AiPromptTrace(null,
            AiModelInfo.embedding(modelId, outputDimensionality),
            AiExecInfo.durationSince(t0, queryTokens = resp.usage.promptTokens, responseTokens = resp.usage.completionTokens),
            AiOutputInfo.output(resp.embeddings.map { AiOutput(other = it.embedding) })
        )
    }

    /** Runs a quick audio transcription for a given file. */
    suspend fun quickTranscribe(modelId: String = AUDIO_WHISPER, audioFile: File): AiPromptTrace {
        settings.checkApiKey()
        if (!audioFile.isAudioFile())
            return AiPromptTrace.invalidRequest(modelId, "Audio file not provided.")

        val t0 = System.currentTimeMillis()
        val resp = client.transcription(TranscriptionRequest(
            model = ModelId(modelId),
            // convert audiofile toa  kotlin path object
            audio = FileSource(Path(audioFile.absolutePath), SystemFileSystem)
        ))
        resp.duration?.let {
            usage.increment(it.toInt(), UsageUnit.AUDIO_SECONDS)
        }

        return AiPromptTrace(null,
            AiModelInfo(modelId),
            AiExecInfo.durationSince(t0),
            AiOutputInfo.text(resp.text)
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

        return AiPromptTrace(
            completionRequest.prompt?.let { PromptInfo(it) },
            completionRequest.toModelInfo(),
            AiExecInfo.durationSince(t0, queryTokens = resp.usage?.promptTokens, responseTokens = resp.usage?.completionTokens),
            AiOutputInfo.text(resp.choices.map { it.text })
        )
    }

    suspend fun chatCompletion(completionRequest: ChatCompletionRequest) =
        chat(completionRequest, multimodal = false).mapOutput { AiOutput(text = it.textContent(ifNone = "(no response)")) }

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

        return AiPromptTrace(
            prompt?.let { PromptInfo(it) },
            completionRequest.toModelInfo(),
            AiExecInfo.durationSince(t0, queryTokens = resp.usage?.promptTokens, responseTokens = resp.usage?.completionTokens),
            outputInfo
        )
    }

    /** Runs an edit request (deprecated API). */
    @Suppress("DEPRECATION")
    suspend fun edit(request: EditsRequest): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.edit(request)
        usage.increment(resp.usage)

        return AiPromptTrace(
            request.toPromptInfo(),
            request.toModelInfo(),
            AiExecInfo.durationSince(t0, queryTokens = resp.usage.promptTokens, responseTokens = resp.usage.completionTokens),
            AiOutputInfo.text(resp.choices.map { it.text })
        )
    }

    /** Runs an image creation request. */
    suspend fun imageURL(imageCreation: ImageCreation): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.imageURL(imageCreation)
        usage.increment(resp.size, UsageUnit.IMAGES)

        return AiPromptTrace(
            null,
            imageCreation.toModelInfo(),
            AiExecInfo.durationSince(t0),
            AiOutputInfo.multimodalMessages(resp.map { MultimodalChatMessage.imageUrl(imageUrl = it.url) })
        )
    }

    /** Runs an image creation request. */
    suspend fun imageJSON(imageCreation: ImageCreation): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.imageJSON(imageCreation)
        usage.increment(resp.size, UsageUnit.IMAGES)

        return AiPromptTrace(
            null,
            imageCreation.toModelInfo(),
            AiExecInfo.durationSince(t0),
            AiOutputInfo.multimodalMessages(resp.map { MultimodalChatMessage.imageBase64(imageBase64 = it.b64JSON) })
        )
    }

    /** Runs a speech request. */
    suspend fun speech(request: SpeechRequest): AiPromptTraceSupport {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.speech(request)

        return AiPromptTrace(
            null,
            request.toModelInfo(),
            AiExecInfo.durationSince(t0),
            AiOutputInfo.other(resp)
        )
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
        info.created = Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    if (info.type == ModelType.UNKNOWN) {
        when {
            "moderation" in id.id -> info.type = ModelType.MODERATION
            "-realtime-" in id.id -> {
                info.type = ModelType.REALTIME_CHAT
                info.inputs = listOf(DataModality.text, DataModality.audio)
                info.outputs = listOf(DataModality.text, DataModality.audio)
            }
            "-audio-" in id.id -> {
                info.type = ModelType.AUDIO_CHAT
                info.inputs = listOf(DataModality.audio)
                info.outputs = listOf(DataModality.audio)
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
