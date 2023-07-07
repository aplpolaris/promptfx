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
package tri.ai.openai.apps

import com.aallam.openai.api.logging.LogLevel
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.memory.*
import tri.ai.openai.*

/** Example of a chat that has a memory of previous conversations. */
class OpenAiMemoryChat {

    val greeting = "You are chatting with GPT-3.5 Turbo (with memory). Say 'bye' to exit."
    val model = COMBO_GPT35
    val chatService = OpenAiChat(modelId = model)

    val persona: BotPersona = HelperPersona("Jack")
    val memory: MemoryService = BotMemory(persona, OpenAiChat(), OpenAiEmbeddingService())

    /**
     * Completes a chat from the user.
     * The primary task is to send the chat history to the chat API and print the result.
     * The secondary task is to process and store a "memory" of the conversation.
     */
    suspend fun doChat(userInput: String): TextChatMessage {
        val userItem = MemoryItem(TextChatRole.User, userInput)
        memory.addChat(userItem)
        val contextualHistory = memory.buildContextualConversationHistory(userItem).map { it.toChatMessage() }
        val personaMessage = listOf(TextChatMessage(TextChatRole.System, persona.getSystemMessage()))
        val response = chatService.chat(personaMessage + contextualHistory).value!!
        memory.addChat(MemoryItem(response))
        memory.saveMemory(interimSave = true)
        return response
    }

    //region INPUT/OUTPUT

    /** Prints the chat message to the console. */
    fun showChat(message: TextChatMessage) {
        println(message.content)
    }

    /** Reads a line of input from the console. */
    fun readUserInput(): String {
        print("> ")
        return readln()
    }

    //endregion

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
            val chatbot = OpenAiMemoryChat()
            runBlocking {
                println(chatbot.greeting)
                chatbot.memory.initMemory()
                var input = chatbot.readUserInput()
                while (input != "bye") {
                    val chat = chatbot.doChat(input)
                    chatbot.showChat(chat)
                    input = chatbot.readUserInput()
                }
                chatbot.memory.saveMemory(interimSave = false)
            }
            println("Goodbye!")
        }

    }
}

