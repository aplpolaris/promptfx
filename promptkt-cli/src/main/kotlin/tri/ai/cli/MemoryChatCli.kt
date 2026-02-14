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
package tri.ai.cli

import com.aallam.openai.api.logging.LogLevel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.TextPlugin
import tri.ai.memory.*
import tri.ai.openai.*
import tri.ai.openai.OpenAiModelIndex.EMBEDDING_ADA
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO_ID
import tri.util.MIN_LEVEL_TO_LOG
import java.util.logging.Level
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    MemoryChatCli().main(args)

/** Example of a chat that has a memory of previous conversations. */
class MemoryChatCli : CliktCommand(name = "chat-memory") {
    private val model by option("--model", help = "Chat model or LLM to use (default $GPT35_TURBO_ID)")
        .default(GPT35_TURBO_ID)
    private val embedding by option("--embedding", help = "Embedding model to use (default $EMBEDDING_ADA)")
        .default(EMBEDDING_ADA)
    private val verbose by option("--verbose", help = "Verbose logging").flag()

    private val greeting
        get() = "You are chatting with $model (with memory). Say 'bye' to exit."
    private val chatModelInst
        get() = TextPlugin.chatModels().first { it.modelId == model }
    private val embeddingModelInst
        get() = TextPlugin.embeddingModels().first { it.modelId == embedding }

    private val persona: BotPersona = HelperPersona("Jack")
    private val memory: MemoryService by lazy { BotMemory(persona, chatModelInst, embeddingModelInst) }

    override fun run() {
        if (verbose) {
            println("Verbose logging enabled.")
            MIN_LEVEL_TO_LOG = Level.FINE
        } else {
            MIN_LEVEL_TO_LOG = Level.WARNING
            OpenAiAdapter.INSTANCE.settings.logLevel = LogLevel.None
        }

        println(greeting)
        memory.initMemory()

        runBlocking {
            var input = readUserInput()
            while (input != "bye") {
                val chat = doChat(input)
                showChat(chat)
                input = readUserInput()
            }
            memory.saveMemory(interimSave = false)
        }
        println("Goodbye!")
        exitProcess(0)
    }

    /**
     * Completes a chat from the user.
     * The primary task is to send the chat history to the chat API and print the result.
     * The secondary task is to process and store a "memory" of the conversation.
     */
    private suspend fun doChat(userInput: String): TextChatMessage {
        val userItem = MemoryItem(MChatRole.User, userInput)
        memory.addChat(userItem)
        val contextualHistory = memory.buildContextualConversationHistory(userItem).map { it.toChatMessage() }
        val personaMessage = listOf(TextChatMessage(MChatRole.System, persona.getSystemMessage()))
        val response = chatModelInst.chat(personaMessage + contextualHistory).firstValue
        memory.addChat(MemoryItem(response.message!!))
        memory.saveMemory(interimSave = true)
        return response.message!!
    }

    //region INPUT/OUTPUT

    /** Prints the chat message to the console. */
    private fun showChat(message: TextChatMessage) {
        println(message.content)
    }

    /** Reads a line of input from the console. */
    private fun readUserInput(): String {
        print("> ")
        return readln()
    }

    //endregion
}

