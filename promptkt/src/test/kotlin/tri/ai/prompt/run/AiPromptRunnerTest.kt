package tri.ai.prompt.run

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.openai.writer
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo

class AiPromptRunnerTest {

    private val defaultTextCompletion = TextPlugin.textCompletionModels().first()

    private val promptInfo = AiPromptInfo(
        "Translate {{text}} into French.",
        mapOf("text" to "Hello, world!")
    )
    private val modelInfo = AiPromptModelInfo(
        "not a model",
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )

    @Test
    @Disabled("Requires OpenAI API key")
    fun testRun() {
        runBlocking {
            val trace = (promptInfo to modelInfo).execute(defaultTextCompletion)
            println("Trace: $trace")
            println("Trace: ${writer.writeValueAsString(trace)}")
        }
    }

}