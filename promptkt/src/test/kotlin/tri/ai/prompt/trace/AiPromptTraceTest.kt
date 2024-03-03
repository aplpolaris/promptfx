package tri.ai.prompt.trace

import org.junit.jupiter.api.Test
import tri.ai.openai.writer

class AiPromptTraceTest {

    private val promptInfo = AiPromptInfo(
        "Translate {{text}} into French.",
        mapOf("text" to "Hello, world!")
    )
    private val modelInfo = AiPromptModelInfo(
        "not a model",
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )

    @Test
    fun testSerializeConfig() {
        println(writer.writeValueAsString(promptInfo))
    }

    @Test
    fun testSerializeTrace() {
        println(writer.writeValueAsString(AiPromptTrace(promptInfo, modelInfo, AiPromptExecInfo(), AiPromptOutputInfo("test output"))))
        println(writer.writeValueAsString(AiPromptTrace(promptInfo, modelInfo, AiPromptExecInfo.error("test error"))))
    }

}