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

import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.core.VisionLanguageChat
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.pips.AiTaskResult

/** Vision chat completion with Gemini models. */
class GeminiVisionLanguageChat(override val modelId: String, val client: GeminiClient = GeminiClient.INSTANCE) :
    VisionLanguageChat {

    override fun toString() = "$modelId (Gemini)"

    override suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double?,
        tokens: Int?,
        stop: List<String>?,
        requestJson: Boolean?
    ): AiTaskResult<TextChatMessage> {
        val response = client.generateContentVision(messages, modelId,
            GenerationConfig(
                maxOutputTokens = tokens ?: 500,
                stopSequences = stop,
                responseMimeType = if (requestJson == true) "application/json" else null
            )
        )
        return response.candidates!!.first().let {
            val role = when (it.content.role) {
                "user" -> TextChatRole.User
                "model" -> TextChatRole.Assistant
                else -> error("Invalid role: ${it.content.role}")
            }
            AiTaskResult.result(TextChatMessage(role, it.content.parts[0].text))
        }
    }

}
