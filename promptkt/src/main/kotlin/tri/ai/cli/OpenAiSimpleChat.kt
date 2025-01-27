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
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.core.TextPlugin
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiModelIndex
import tri.util.MIN_LEVEL_TO_LOG
import java.util.logging.Level
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    OpenAiSimpleChat().main(args)

/**
 * Command-line executable for chatting with GPT-3.5 Turbo.
 */
class OpenAiSimpleChat: CliktCommand(name = "openai-chat") {
    private val completionModel by option("--completionModel", help = "Completion model to use.")
        .default(OpenAiModelIndex.GPT35_TURBO_ID)
    private val historySize by option("--historySize", help = "Maximum chat history size.")
        .int()
        .default(10)

    private val completionModelInst
        get() = TextPlugin.chatModels().first { it.modelId == completionModel }


    override fun run() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
        MIN_LEVEL_TO_LOG = Level.WARNING

        runBlocking {
            // chat with the user until they say "bye"
//            OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
//            val client = OpenAI(OpenAIConfig(OpenAiClient.INSTANCE.settings.apiKey, LoggingConfig(LogLevel.None)))
//            val model = GPT35_TURBO
            val chatHistory = mutableListOf<TextChatMessage>()

            println("You are chatting with $completionModel. Say 'bye' to exit.")
            print("> ")
            var input = readln()
            while (input != "bye") {
                chatHistory.add(TextChatMessage(TextChatRole.User, input))
                val response = completionModelInst.chat(chatHistory)
                val message = response.firstValue
                println(message.content)
                chatHistory.add(TextChatMessage(TextChatRole.Assistant, message.content))
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
