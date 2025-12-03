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
package tri.ai.prompt.trace.batch

import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.TextPlugin
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.PromptInfo.Companion.filled

/** Configuration required for executing a text completion prompt. */
class AiPromptRunConfig(
    val promptInfo: PromptInfo,
    val modelInfo: AiModelInfo,
    val modelLookup: (String) -> TextChat = { TextPlugin.chatModel(it) }
) {
    override fun toString() =
        "AiPromptRunConfig(promptInfo=$promptInfo, modelInfo=$modelInfo)"

    /**
     * Executes a text chat completion with a single configuration.
     * Overwrites the model id in the configuration to match the model.
     * @param chat the chat model
     * @return trace of the execution
     */
    suspend fun execute(chat: TextChat): AiPromptTrace {
        modelInfo.modelId = chat.modelId
        val promptText = promptInfo.filled()
        val result = chat.chat(promptText, modelInfo)
        return result.copy(promptInfo = promptInfo).mapOutput { AiOutput(text = it.message!!.content!!) }
    }

    /**
     * Executes a single text completion query, with model parameters encoded in [modelInfo].
     */
    private suspend fun TextChat.chat(text: String, modelInfo: AiModelInfo) =
        chat(
            messages = listOf(TextChatMessage.user(text)),
            tokens = modelInfo.modelParams[AiModelInfo.MAX_TOKENS] as? Int,
            variation = modelInfo.toVariation(),
            stop = modelInfo.modelParams[AiModelInfo.STOP] as? List<String>
                ?: (modelInfo.modelParams[AiModelInfo.STOP] as? String)?.let { listOf(it) },
            numResponses = modelInfo.modelParams[AiModelInfo.NUM_RESPONSES] as? Int
        )

    private fun AiModelInfo.toVariation() = tri.ai.core.MChatVariation(
        seed = (modelParams[AiModelInfo.SEED] as? Number)?.toInt(),
        temperature = modelParams[AiModelInfo.TEMPERATURE] as? Double,
        topP = modelParams[AiModelInfo.TOP_P] as? Double,
        presencePenalty = modelParams[AiModelInfo.PRESENCE_PENALTY] as? Double,
        frequencyPenalty = modelParams[AiModelInfo.FREQUENCY_PENALTY] as? Double,
    )

}