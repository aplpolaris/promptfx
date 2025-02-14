/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.ai.cli

import org.junit.jupiter.api.Test
import kotlin.io.path.deleteIfExists
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

        // delete temporary file
        inputFile.deleteIfExists()
        outputFile.deleteIfExists()
        outputFile2.deleteIfExists()
    }
}
