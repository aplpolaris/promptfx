/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
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
package tri.promptfx.ui

import tornadofx.Component
import tornadofx.ScopedInstance
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.openai.COMBO_GPT35
import tri.ai.openai.OpenAiChat
import tri.ai.openai.OpenAiClient

/** General-purpose tool that generates responses to chat messages. */
abstract class ChatDriver : ScopedInstance, Component() {

    /** System message, if present will be included with all chats. */
    var systemMessage: ChatEntry? = null
    /** Number of chats from history to include in chat driver call. */
    var chatHistorySize = 1

    /** General name to show for the user. */
    abstract val userName: String
    /** General name to show for the system response. */
    abstract val systemName: String

    /** Generate a response based on a sequence of prior messages. */
    abstract suspend fun chat(messages: List<ChatEntry>): ChatEntry

}

/** Driver based on OpenAI's GPT API. */
class OpenAiChatDriver : ChatDriver() {

    private val inst = OpenAiClient.INSTANCE
    private val chatter = OpenAiChat(COMBO_GPT35, inst)

    override var userName = System.getProperty("user.name")
    override var systemName = "ChatGPT (${chatter.modelId})"

    override suspend fun chat(messages: List<ChatEntry>): ChatEntry {
        val inputChats = listOfNotNull(systemMessage) + messages.takeLast(chatHistorySize)
        val response = chatter.chat(inputChats.mapNotNull { it.toTextChatMessage() })
        return ChatEntry(systemName, response.value?.content ?: "No response",
            response.value?.role?.toChatRoleStyle() ?: ChatEntryRole.ERROR)
    }

}
