package tri.ai.openai.apps

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import kotlinx.coroutines.runBlocking
import tri.ai.openai.COMBO_GPT35
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiSettings

@OptIn(BetaOpenAI::class)
object OpenAiSimpleChat {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            // chat with the user until they say "bye"
            OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None
            val client = OpenAI(OpenAIConfig(OpenAiClient.INSTANCE.settings.apiKey, LogLevel.None))
            val model = COMBO_GPT35
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
                val message = response.choices[0].message!!.content!!.trim()
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
    }
}