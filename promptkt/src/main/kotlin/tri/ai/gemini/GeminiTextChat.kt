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

import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.gemini.GeminiModelIndex.GEMINI_PRO
import tri.ai.pips.AiTaskResult

/** Text chat with Gemini models. */
class GeminiTextChat(override val modelId: String = GEMINI_PRO, val client: GeminiClient = GeminiClient.INSTANCE) :
    TextChat {

    override fun toString() = "$modelId (Gemini)"

    override suspend fun chat(messages: List<TextChatMessage>, tokens: Int?, stop: List<String>?, requestJson: Boolean?, numResponses: Int?) =
        client.generateContent(messages, modelId,
            GenerationConfig(
                maxOutputTokens = tokens ?: 500,
                stopSequences = stop,
                responseMimeType = if (requestJson == true) "application/json" else null
            )
        ).candidates!!.first().let {
            val role = when (it.content.role) {
                "user" -> TextChatRole.User
                "model" -> TextChatRole.Assistant
                else -> error("Invalid role: ${it.content.role}")
            }
            AiTaskResult.results(
                it.content.parts.map { part ->
                    TextChatMessage(role, part.text)
                }
            )
        }

}
