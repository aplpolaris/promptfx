package tri.ai.prompt.run

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.openai.writer

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
                println("Trace: $it")
                println("Trace: ${writer.writeValueAsString(it)}")
            }
        }
    }

}