package tri.ai.tool

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import kotlinx.serialization.SerializationException
import tri.ai.openai.OpenAiClient
import tri.util.*

/**
 * Executes a prompt using tools and OpenAI. This will attempt to use tools in sequence as needed until a response
 * is achieved, at which point the system will return the final response. This may also ask the user to clarify their
 * query if needed.
 */
@OptIn(BetaOpenAI::class)
class JsonToolExecutor(val client: OpenAiClient, val model: String, val tools: List<JsonTool>) {

    private val functions = tools.mapNotNull {
        try {
            val params = it.jsonSchemaAsParameters()
            ChatCompletionFunction(it.name, it.description, params)
        } catch (x: SerializationException) {
            println(x)
            null
        }
    }

    suspend fun execute(query: String) {
        println("User Question: $ANSI_YELLOW$query$ANSI_RESET")
        val messages = mutableListOf(
            ChatMessage(ChatRole.System, SYSTEM_MESSAGE_1),
            ChatMessage(ChatRole.User, query)
        )

        var response = client.chat(ChatCompletionRequest(
            model = ModelId(this@JsonToolExecutor.model),
            messages = messages,
            functions = this@JsonToolExecutor.functions.ifEmpty { null }
        )).value!!
        var functionCall = response.functionCall

        while (functionCall != null) {
            // print interim results
            println("Call Function: $ANSI_CYAN${functionCall.name}$ANSI_RESET with parameters $ANSI_CYAN${functionCall.arguments}$ANSI_RESET")
            val tool = tools.firstOrNull { it.name == functionCall!!.name }
            if (tool == null) {
                println("${ANSI_RED}Unknown tool: ${functionCall.name}$ANSI_RESET")
            }
            val json = functionCall.tryJson()
            if (json == null) {
                println("${ANSI_RED}Invalid JSON: ${functionCall.name}$ANSI_RESET")
            }
            if (tool != null && json != null) {
                val result = tool.run(json)
                println("Result: $ANSI_GREEN${result}$ANSI_RESET")

                // add result to message history and call again
                messages += ChatMessage(response.role, name = response.name, content = response.content ?: "", functionCall = response.functionCall)
                messages += ChatMessage(ChatRole.Function, name = tool.name, content = result)
                response = client.chat(ChatCompletionRequest(
                    model = ModelId(this@JsonToolExecutor.model),
                    messages = messages,
                    functions = this@JsonToolExecutor.functions.ifEmpty { null }
                )).value!!
                functionCall = response.functionCall
            } else {
                functionCall = null
            }
        }

        println("Final Response: $ANSI_GREEN${response.content}$ANSI_RESET")
    }

    companion object {
        private const val SYSTEM_MESSAGE_1 =
            "Don't make assumptions about what values to plug into functions. " +
                    "Ask for clarification if a user request is ambiguous. " +
                    "Don't attempt to answer questions that are outside the scope of the functions. "

        private fun FunctionCall.tryJson() = try {
            argumentsAsJson()
        } catch (x: SerializationException) {
            null
        }
    }

}