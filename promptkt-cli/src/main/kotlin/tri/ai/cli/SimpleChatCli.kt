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
package tri.ai.cli

import com.aallam.openai.api.logging.LogLevel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.TextPlugin
import tri.ai.openai.OpenAiAdapter
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO_ID
import tri.util.MIN_LEVEL_TO_LOG
import java.util.logging.Level
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    SimpleChatCli().main(args)

/**
 * Command-line executable for chatting with GPT-3.5 Turbo.
 */
class SimpleChatCli : CliktCommand(name = "chat-simple") {
    private val model by option("--model", help = "Chat model or LLM to use (default $GPT35_TURBO_ID)")
        .default(GPT35_TURBO_ID)
    private val historySize by option("--historySize", help = "Maximum chat history size (default 10)")
        .int()
        .default(10)
    private val verbose by option("--verbose", help = "Verbose logging").flag()

    private val greeting
        get() = "You are chatting with $model. Say 'bye' to exit."
    private val chatModelInst
        get() = TextPlugin.chatModels().first { it.modelId == model }


    override fun run() {
        if (verbose) {
            println("Verbose logging enabled.")
            MIN_LEVEL_TO_LOG = Level.FINE
        } else {
            MIN_LEVEL_TO_LOG = Level.WARNING
            OpenAiAdapter.INSTANCE.settings.logLevel = LogLevel.None
        }

        runBlocking {
            // chat with the user until they say "bye"
            val chatHistory = mutableListOf<TextChatMessage>()

            println(greeting)
            print("> ")
            var input = readln()
            while (input != "bye") {
                chatHistory.add(TextChatMessage(MChatRole.User, input))
                val response = chatModelInst.chat(chatHistory)
                val message = response.firstValue.message!!
                println(message)
                chatHistory.add(TextChatMessage(MChatRole.Assistant, message.content))
                while (chatHistory.size > historySize) {
                    chatHistory.removeAt(0)
                }
                print("> ")
                input = readln()
            }
            println("Goodbye!")
        }
        exitProcess(0)
    }
}
