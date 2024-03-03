package tri.ai.prompt.run

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.openai.mapper
import tri.ai.openai.writer
import tri.ai.prompt.trace.AiPromptTraceDatabase
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo

class AiPromptBatchTest {

    private val defaultTextCompletion = TextPlugin.textCompletionModels().first()

    private val batch = AiPromptBatchCyclic().apply {
        model = defaultTextCompletion.modelId
        prompt = listOf("Translate {{text}} into {{language}}.")
        promptParams = mapOf("text" to "Hello, world!", "language" to listOf("French", "German"))
        runs = 2
    }

    @Test
    fun testSerialize() {
        println(writer.writeValueAsString(batch))
    }

    @Test
    @Disabled("Requires OpenAI API key")
    fun testExecute() {
        runBlocking {
            batch.execute().onEach {
                println("Trace: ${writer.writeValueAsString(it)}")
            }
        }
    }

    @Test
    @Disabled("Requires OpenAI API key")
    fun testBatchExecuteDatabase() {
        runBlocking {
            val batch = AiPromptBatchCyclic.repeat(
                AiPromptInfo("Generate a random number between 1 and 100."),
                AiPromptModelInfo(defaultTextCompletion.modelId),
                4
            )
            val result = batch.execute()
            val db = AiPromptTraceDatabase().apply {
                addTraces(result)
            }
            val output = writer.writeValueAsString(db)
            val db2 = mapper.readValue<AiPromptTraceDatabase>(output)
            assertEquals(db.traces, db2.traces)
            assertEquals(db.prompts, db2.prompts)
            assertEquals(db.models, db2.models)
            assertEquals(db.execs, db2.execs)
            println(writer.writeValueAsString(db2))
        }
    }

}