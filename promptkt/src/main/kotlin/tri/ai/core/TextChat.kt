/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.core

import tri.ai.pips.AiTaskResult

/** Interface for chat completion. */
interface TextChat {

    val modelId: String

    /** Completes user text. */
    suspend fun chat(
        messages: List<TextChatMessage>,
        tokens: Int? = 1000
    ): AiTaskResult<TextChatMessage>

}

/** A single message in a chat. */
class TextChatMessage(val role: TextChatRole, val content: String)

/** The role of a chat message. */
enum class TextChatRole {
    System, User, Assistant
}
