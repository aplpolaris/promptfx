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
import com.aallam.openai.api.model.ModelId
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
import tri.ai.openai.OpenAiModelIndex.DALLE2_ID
import tri.ai.openai.OpenAiModelIndex.EMBEDDING_ADA
import tri.ai.openai.OpenAiModelIndex.IMAGE_DALLE2
import tri.ai.pips.UsageUnit
import tri.ai.prompt.trace.*
import java.io.File
import java.util.*

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
    suspend fun quickEmbedding(modelId: String = EMBEDDING_ADA, outputDimensionality: Int? = null, inputs: List<String>): AiPromptTrace<List<List<Double>>> {
        checkApiKey()
        return quickEmbedding(modelId, outputDimensionality, *inputs.toTypedArray())
    }

    /** Runs an embedding using ADA embedding model. */
    suspend fun quickEmbedding(modelId: String, outputDimensionality: Int? = null, vararg inputs: String): AiPromptTrace<List<List<Double>>> {
        checkApiKey()

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
            AiOutputInfo.output(resp.embeddings.map { it.embedding })
        )
    }

    /** Runs a quick audio transcription for a given file. */
    suspend fun quickTranscribe(modelId: String = AUDIO_WHISPER, audioFile: File): AiPromptTrace<String> {
        checkApiKey()
        if (!audioFile.isAudioFile())
            return AiPromptTrace.invalidRequest(modelId, "Audio file not provided.")

        val t0 = System.currentTimeMillis()
        val resp = client.transcription(TranscriptionRequest(
            model = ModelId(modelId),
            audio = FileSource(audioFile.toOkioPath(), FileSystem.SYSTEM)
        ))
        resp.duration?.let {
            usage.increment(it.toInt(), UsageUnit.AUDIO_SECONDS)
        }

        return AiPromptTrace(null,
            AiModelInfo(modelId),
            AiExecInfo.durationSince(t0),
            AiOutputInfo.output(resp.text)
        )
    }

    //endregion

    //region DIRECT API CALLS

    /** Runs a text completion request. */
    suspend fun completion(completionRequest: CompletionRequest): AiPromptTrace<String> {
        checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.completion(completionRequest)
        usage.increment(resp.usage)

        return AiPromptTrace(
            completionRequest.prompt?.let { AiPromptInfo(it) },
            completionRequest.toModelInfo(),
            AiExecInfo.durationSince(t0, queryTokens = resp.usage?.promptTokens, responseTokens = resp.usage?.completionTokens),
            AiOutputInfo(resp.choices.map { it.text })
        )
    }

    /** Runs a text completion request using a chat model. */
    suspend fun chatCompletion(completionRequest: ChatCompletionRequest) =
        chat(completionRequest).mapOutput { it.content ?: "(no response)" }

    /** Runs a chat response. */
    suspend fun chat(completionRequest: ChatCompletionRequest): AiPromptTrace<ChatMessage> {
        checkApiKey()

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

        return AiPromptTrace(
            prompt?.let { AiPromptInfo(it) },
            completionRequest.toModelInfo(),
            AiExecInfo.durationSince(t0, queryTokens = resp.usage?.promptTokens, responseTokens = resp.usage?.completionTokens),
            AiOutputInfo(resp.choices.map { it.message })
        )
    }

    /** Runs an edit request (deprecated API). */
    @Suppress("DEPRECATION")
    suspend fun edit(request: EditsRequest): AiPromptTrace<String> {
        checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.edit(request)
        usage.increment(resp.usage)

        return AiPromptTrace(
            request.toPromptInfo(),
            request.toModelInfo(),
            AiExecInfo.durationSince(t0, queryTokens = resp.usage.promptTokens, responseTokens = resp.usage.completionTokens),
            AiOutputInfo(resp.choices.map { it.text })
        )
    }

    /** Runs an image creation request. */
    suspend fun imageURL(imageCreation: ImageCreation): AiPromptTrace<String> {
        checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.imageURL(imageCreation)
        usage.increment(resp.size, UsageUnit.IMAGES)

        return AiPromptTrace(
            null,
            imageCreation.toModelInfo(),
            AiExecInfo.durationSince(t0),
            AiOutputInfo(resp.map { it.url })
        )
    }

    /** Runs a speech request. */
    suspend fun speech(request: SpeechRequest): AiPromptTraceSupport<ByteArray> {
        checkApiKey()

        val t0 = System.currentTimeMillis()
        val resp = client.speech(request)

        return AiPromptTrace(
            null,
            request.toModelInfo(),
            AiExecInfo.durationSince(t0),
            AiOutputInfo.output(resp)
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

    private fun EditsRequest.toPromptInfo() = AiPromptInfo.info(instruction,
        AiPromptInfo.INSTRUCTION to instruction,
        AiPromptInfo.INPUT to input
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

    companion object {
        val INSTANCE by lazy { OpenAiClient(OpenAiSettings()) }
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
