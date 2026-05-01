/*-
 * #%L
 * tri.promptfx:promptfx-sample-textplugin
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
package tri.promptfx.sample.textplugin

import tri.ai.core.*
import tri.ai.prompt.trace.AiEnvInfo
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiTaskTrace
import java.io.File
import java.net.URI

/**
 * Sample plugin demonstrating [AiModelProvider] implementation.
 *
 * Provides stub implementations of all seven model capability types. Use this as a template
 * when creating a new provider — replace each stub with real API calls and update [modelInfo]
 * with accurate metadata. Register the provider class name in:
 *   META-INF/services/tri.ai.core.AiModelProvider
 *
 * Providers that do not support a capability type should return [emptyList] for that method
 * (the default in [AiModelProvider]); do NOT return a model that throws on every call.
 */
class SampleAiModelProvider : AiModelProvider {

    override fun isApiConfigured() = true

    override fun modelSource() = MODEL_SOURCE

    override fun modelInfo() = listOf(
        ModelInfo("sample-chat-v1", ModelType.TEXT_CHAT, modelSource()).apply {
            metadata.name = "Sample Chat Model"
            metadata.description = "Echoes the last user message"
            params["inputTokenLimit"] = 1000
            params["outputTokenLimit"] = 1000
        },
        ModelInfo("sample-multimodal-v1", ModelType.TEXT_CHAT, modelSource()).apply {
            metadata.name = "Sample Multimodal Model"
            metadata.description = "Echoes text content from multimodal messages"
            params["inputTokenLimit"] = 1000
            params["outputTokenLimit"] = 1000
        },
        ModelInfo("sample-echo-v1", ModelType.TEXT_COMPLETION, modelSource()).apply {
            metadata.name = "Sample Completion Model"
            metadata.description = "Echoes the input text with a prefix"
            params["inputTokenLimit"] = 1000
            params["outputTokenLimit"] = 1000
        },
        ModelInfo("sample-embedding-v1", ModelType.TEXT_EMBEDDING, modelSource()).apply {
            metadata.name = "Sample Embedding Model"
            metadata.description = "Returns a fixed-size zero vector for each input"
            params["outputDimensionality"] = EMBEDDING_DIM
        },
        ModelInfo("sample-image-v1", ModelType.IMAGE_GENERATOR, modelSource()).apply {
            metadata.name = "Sample Image Generator"
            metadata.description = "Returns a placeholder data: URI"
        },
        ModelInfo("sample-tts-v1", ModelType.TEXT_TO_SPEECH, modelSource()).apply {
            metadata.name = "Sample TTS Model"
            metadata.description = "Returns empty audio bytes"
        },
        ModelInfo("sample-stt-v1", ModelType.SPEECH_TO_TEXT, modelSource()).apply {
            metadata.name = "Sample STT Model"
            metadata.description = "Returns a fixed placeholder transcription"
        }
    )

    override fun chatModels() = listOf(SampleChatModel())
    override fun multimodalModels() = listOf(SampleMultimodalChatModel())
    override fun textCompletionModels() = listOf(SampleTextCompletionModel())
    override fun embeddingModels() = listOf(SampleEmbeddingModel())
    override fun imageGeneratorModels() = listOf(SampleImageGenerator())
    override fun textToSpeechModels() = listOf(SampleTextToSpeechModel())
    override fun speechToTextModels() = listOf(SampleSpeechToTextModel())

    override fun close() { }

    companion object {
        const val MODEL_SOURCE = "SampleText"
        const val EMBEDDING_DIM = 8
    }
}

// --- TextCompletion ---

/** Echoes input text with a prefix. */
class SampleTextCompletionModel : TextCompletion {
    override val modelId = "sample-echo-v1"
    override val modelSource = SampleAiModelProvider.MODEL_SOURCE
    override fun toString() = modelDisplayName()

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiTaskTrace = AiTaskTrace(
        env = AiEnvInfo(AiModelInfo(modelId)),
        exec = AiExecInfo(),
        output = AiOutputInfo.text("Sample Echo: $text")
    )
}

// --- TextChat ---

/** Echoes the last user message as an assistant reply. */
class SampleChatModel : TextChat {
    override val modelId = "sample-chat-v1"
    override val modelSource = SampleAiModelProvider.MODEL_SOURCE
    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ): AiTaskTrace {
        val last = messages.lastOrNull()?.content ?: "No message provided"
        val reply = TextChatMessage(MChatRole.Assistant, "Sample response to: $last")
        return AiTaskTrace(
            env = AiEnvInfo(AiModelInfo(modelId)),
            exec = AiExecInfo(),
            output = AiOutputInfo.messages(listOf(reply))
        )
    }
}

// --- MultimodalChat ---

/** Echoes text content extracted from the last multimodal message. */
class SampleMultimodalChatModel : MultimodalChat {
    override val modelId = "sample-multimodal-v1"
    override val modelSource = SampleAiModelProvider.MODEL_SOURCE
    override fun toString() = modelDisplayName()

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiTaskTrace {
        val lastText = messages.lastOrNull()
            ?.content?.filterIsInstance<MChatMessagePart>()
            ?.firstOrNull { it.partType == MPartType.TEXT }
            ?.text ?: "No text content"
        val reply = MultimodalChatMessage.text(MChatRole.Assistant, "Sample multimodal response to: $lastText")
        return AiTaskTrace(
            env = AiEnvInfo(AiModelInfo(modelId)),
            exec = AiExecInfo(),
            output = AiOutputInfo.multimodalMessage(reply)
        )
    }

    override suspend fun chat(message: MultimodalChatMessage, parameters: MChatParameters) =
        chat(listOf(message), parameters)

    override fun close() { }
}

// --- EmbeddingModel ---

/** Returns a zero vector of fixed dimensionality for each input string. */
class SampleEmbeddingModel : EmbeddingModel {
    override val modelId = "sample-embedding-v1"
    override val modelSource = SampleAiModelProvider.MODEL_SOURCE
    override fun toString() = modelDisplayName()

    override suspend fun calculateEmbedding(
        text: List<String>,
        outputDimensionality: Int?
    ): List<List<Double>> {
        val dim = outputDimensionality ?: SampleAiModelProvider.EMBEDDING_DIM
        return text.map { List(dim) { 0.0 } }
    }
}

// --- ImageGenerator ---

/** Returns a 1×1 transparent PNG as a data: URI for each requested image. */
class SampleImageGenerator : ImageGenerator {
    override val modelId = "sample-image-v1"
    override val modelSource = SampleAiModelProvider.MODEL_SOURCE
    override fun toString() = modelDisplayName()

    override suspend fun generateImage(text: String, params: ImageGenerationParams): List<URI> {
        val count = params.numResponses ?: 1
        // 1×1 transparent PNG (base64-encoded)
        val placeholder = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        return List(count) { URI.create(placeholder) }
    }
}

// --- TextToSpeechModel ---

/** Returns an empty ByteArray as the audio output. */
class SampleTextToSpeechModel : TextToSpeechModel {
    override val modelId = "sample-tts-v1"
    override val modelSource = SampleAiModelProvider.MODEL_SOURCE
    override fun toString() = modelDisplayName()

    override suspend fun speech(text: String, voice: String?, speed: Double?): AiTaskTrace =
        AiTaskTrace(
            env = AiEnvInfo(AiModelInfo(modelId)),
            exec = AiExecInfo(),
            output = AiOutputInfo.output(AiOutput.Other(ByteArray(0)))
        )
}

// --- SpeechToTextModel ---

/** Returns a fixed placeholder transcription regardless of input file. */
class SampleSpeechToTextModel : SpeechToTextModel {
    override val modelId = "sample-stt-v1"
    override val modelSource = SampleAiModelProvider.MODEL_SOURCE
    override fun toString() = modelDisplayName()

    override suspend fun transcribe(audioFile: File, prompt: String?): AiTaskTrace =
        AiTaskTrace(
            env = AiEnvInfo(AiModelInfo(modelId)),
            exec = AiExecInfo(),
            output = AiOutputInfo.text("Sample transcription of: ${audioFile.name}")
        )
}
