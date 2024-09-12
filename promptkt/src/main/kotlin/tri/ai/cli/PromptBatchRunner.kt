/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

import kotlinx.coroutines.runBlocking
import tri.ai.openai.jsonWriter
import tri.ai.openai.yamlWriter
import tri.ai.pips.*
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.util.*
import java.io.File
import kotlin.system.exitProcess

/** Command-line runner for executing a batch of prompt runs. */
object PromptBatchRunner {
    @JvmStatic
    fun main(args: Array<String>) {
//        val args = arrayOf("D:\\data\\chatgpt\\prompt_batch_test.json", "D:\\data\\chatgpt\\prompt_batch_test_result.yaml", "--database")
        val useArgs = args
        println("""
                $ANSI_GREEN
                Arguments expected:
                  <input file> <output file> <options>
                Options:
                  --database
                $ANSI_RESET
            """.trimIndent())

        if (useArgs.size < 2)
            exitProcess(0)

        val input = useArgs[0]
        val inputFile = File(input)

        val output = useArgs[1]
        val outputFile = File(output)

        if (!checkExtension(inputFile, "json", "yaml", "yml") ||
            !checkExtension(outputFile, "json", "yaml", "yml")
        )
            exitProcess(0)

        val jsonIn = inputFile.extension == "json"
        val jsonOut = outputFile.extension == "json"
        val database = useArgs.contains("--database")

        println("${ANSI_CYAN}Reading prompt batch from ${inputFile}...$ANSI_RESET")
        val batch = try {
            if (jsonIn)
                AiPromptBatchCyclic.fromJson(inputFile.readText())
            else
                AiPromptBatchCyclic.fromYaml(inputFile.readText())
        } catch (x: Exception) {
            println("Error reading input file: $x")
            exitProcess(1)
        }

        println("${ANSI_CYAN}Executing prompt batch with ${batch.runs} runs...$ANSI_RESET")
        val result = runBlocking {
            batch.plan().execute<String>(IgnoreMonitor).finalResult
        }
        println("${ANSI_CYAN}Processing complete.$ANSI_RESET")

        val writer = if (jsonOut) jsonWriter else yamlWriter
        val outputObject: Any = if (database) AiPromptTraceDatabase(listOf(result)) else result
        writer.writeValue(outputFile, outputObject)

        println("${ANSI_CYAN}Output written to $output.$ANSI_RESET")
        exitProcess(0)
    }

    private fun checkExtension(file: File, vararg extensions: String): Boolean {
        if (file.extension !in extensions) {
            println("Invalid file extension: $file. Must be one of: ${extensions.joinToString(", ")}.")
            return false
        }
        return true
    }
}