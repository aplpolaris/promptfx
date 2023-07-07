package tri.ai.prompt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AiPromptLibraryTest {

    @Test
    fun testPromptExists() {
        val prompt = AiPromptLibrary.INSTANCE.prompts["question-answer"]
        Assertions.assertNotNull(prompt)
    }

    @Test
    fun testPromptFill() {
        val prompt = AiPromptLibrary.INSTANCE.prompts["question-answer"]
        val result = prompt!!.instruct(instruct = "What is the meaning of life?",
            input = "42")
        println(result)
        Assertions.assertEquals(161, result.length)
    }

}