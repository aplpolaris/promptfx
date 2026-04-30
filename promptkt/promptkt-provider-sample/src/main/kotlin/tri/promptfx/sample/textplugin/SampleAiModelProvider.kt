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
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiTaskTrace

/** 
 * Sample plugin demonstrating [AiModelProvider] implementation.
 * This provides a simple echo-based AI model for demonstration purposes.
 */
class SampleAiModelProvider : AiModelProvider {

    override fun isApiConfigured() = true

    override fun modelSource() = MODEL_SOURCE

    override fun modelInfo() = listOf(
        ModelInfo("sample-echo-v1", ModelType.TEXT_COMPLETION, modelSource()).apply {
            metadata.name = "Sample Echo Model"
            metadata.description = "A simple echo model that returns the input text with a prefix"
            params["inputTokenLimit"] = 1000
            params["outputTokenLimit"] = 1000
        },
        ModelInfo("sample-chat-v1", ModelType.TEXT_CHAT, modelSource()).apply {
            metadata.name = "Sample Chat Model"
            metadata.description = "A simple chat model that echoes messages"
            params["inputTokenLimit"] = 1000
            params["outputTokenLimit"] = 1000
        }
    )

    override fun embeddingModels() = emptyList<EmbeddingModel>()

    override fun chatModels() = listOf(SampleChatModel())

    override fun multimodalModels() = emptyList<MultimodalChat>()

    override fun textCompletionModels() = listOf(SampleTextCompletionModel())

    override fun imageGeneratorModels() = emptyList<ImageGenerator>()

    override fun close() {
        // No resources to close
    }

    companion object {
        /** Model source identifier for the SampleText plugin. */
        const val MODEL_SOURCE = "SampleText"
    }
}

/** Sample text completion model that echoes input with a prefix. */
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
    ): AiTaskTrace {
        val response = "Sample Echo: $text"
        return AiTaskTrace(
            env = AiEnvInfo(AiModelInfo(modelId)),
            exec = AiExecInfo(),
            output = AiOutputInfo.text(response)
        )
    }
}

/** Sample chat model that echoes the last message. */
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
        val lastMessage = messages.lastOrNull()?.content ?: "No message provided"
        val response = TextChatMessage(MChatRole.Assistant, "Sample response to: $lastMessage")
        return AiTaskTrace(
            env = AiEnvInfo(AiModelInfo(modelId)),
            exec = AiExecInfo(),
            output = AiOutputInfo.messages(listOf(response))
        )
    }
}
