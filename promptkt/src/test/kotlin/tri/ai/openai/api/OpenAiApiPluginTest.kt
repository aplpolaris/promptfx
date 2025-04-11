package tri.ai.openai.api

import org.junit.jupiter.api.Test

class OpenAiApiPluginTest {

    @Test
    fun testModels() {
        println(OpenAiApiPlugin().endpoints())
        OpenAiApiPlugin().modelInfoByEndpoint().forEach { (e, models) ->
            println("$e: ${models.size} models")
            models.forEach { println(it) }
        }
    }

}