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

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import kotlinx.coroutines.runBlocking
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO
import kotlin.system.exitProcess

/**
 * Command-line executable for chatting with GPT-3.5 Turbo.
 */
object OpenAiSimpleChat {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            // chat with the user until they say "bye"
            OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
            val client = OpenAI(OpenAIConfig(OpenAiClient.INSTANCE.settings.apiKey, LoggingConfig(LogLevel.None)))
            val model = GPT35_TURBO
            val historyLimit = 10
            val chatHistory = mutableListOf<ChatMessage>()

            println("You are chatting with GPT-3.5 Turbo. Say 'bye' to exit.")
            print("> ")
            var input = readln()
            while (input != "bye") {
                chatHistory.add(ChatMessage(ChatRole.User, input))
                val response = client.chatCompletion(
                    ChatCompletionRequest(ModelId(model), chatHistory)
                )
                val message = response.choices[0].message.content!!.trim()
                println(message)
                chatHistory.add(ChatMessage(ChatRole.Assistant, message))
                while (chatHistory.size > historyLimit) {
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
