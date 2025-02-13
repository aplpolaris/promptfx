package tri.ai.cli

import org.junit.jupiter.api.Test
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PromptBatchRunnerTest {
    @Test
    fun testRun() {
        val input = """
            ---
            id: test
            model: gpt-3.5-turbo
            prompt:
              - Give me a friendly saying in {{language}}.
            promptParams:
              language: [ French, German, Japanese ]
            runs: 6
        """.trimIndent()

        // write input to temporary file
        val inputFile = kotlin.io.path.createTempFile("prompt-batch-input", ".yaml")
        inputFile.writeText(input)
        val outputFile = kotlin.io.path.createTempFile("prompt-batch-output", ".yaml")
        val outputFile2 = kotlin.io.path.createTempFile("prompt-batch-output", ".yaml")
        PromptBatchRunner().main(arrayOf(inputFile.toString(), outputFile.toString()))
        println(outputFile.readText())
        PromptBatchRunner().main(arrayOf(inputFile.toString(), outputFile2.toString(), "--database"))
        println(outputFile2.readText())
    }
}