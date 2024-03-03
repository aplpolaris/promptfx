package tri.ai.prompt.trace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.ai.openai.jsonWriter

class AiPromptTraceDatabaseTest {

    private val promptInfo = AiPromptInfo(
        "Translate {{text}} into French.",
        mapOf("text" to "Hello, world!")
    )
    private val modelInfo = AiPromptModelInfo(
        "not a model",
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )
    private val execInfo1 = AiPromptExecInfo()
    private val execInfo2 = AiPromptExecInfo.error("test error")
    private val outputInfo1 = AiPromptOutputInfo("test output")
    private val outputInfo2 = AiPromptOutputInfo()

    @Test
    fun testSerialize() {
        val db = AiPromptTraceDatabase().apply {
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo1, outputInfo1))
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo1, outputInfo1))
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo2, outputInfo2))
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo1, outputInfo2))
        }
        assertEquals(4, db.traces.size)
        assertEquals(1, db.prompts.size)
        assertEquals(1, db.models.size)
        assertEquals(2, db.execs.size)
        assertEquals(2, db.outputs.size)

        println(jsonWriter.writeValueAsString(db))
    }

}