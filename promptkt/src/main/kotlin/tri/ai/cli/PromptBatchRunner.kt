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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tri.ai.core.TextPlugin
import tri.ai.openai.jsonWriter
import tri.ai.openai.yamlWriter
import tri.ai.pips.*
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.util.*
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    PromptBatchRunner().main(args)

/** Command-line runner for executing a batch of prompt runs. */
class PromptBatchRunner : CliktCommand(name = "prompt-batch") {
    private val inputFile by argument(help = "input file")
        .file(mustExist = true, canBeDir = false)
        .validate { checkExtension(it, "json", "yaml", "yml") }
    private val outputFile by argument(help = "output file")
        .file(mustExist = false, canBeDir = false)
        .validate { checkExtension(it, "json", "yaml", "yml") }
    private val database by option("--database", help = "Output as database format")
        .flag()

    override fun run() {
        val jsonIn = inputFile.extension == "json"
        val jsonOut = outputFile.extension == "json"

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
            batch.plan { TextPlugin.textCompletionModel(it) }.execute(IgnoreMonitor).finalResult
        }
        println("${ANSI_CYAN}Processing complete.$ANSI_RESET")

        val writer = if (jsonOut) jsonWriter else yamlWriter
        val outputObject: Any = if (database) AiPromptTraceDatabase(listOf(result)) else result
        writer.writeValue(outputFile, outputObject)

        println("${ANSI_CYAN}Output written to $outputFile.$ANSI_RESET")
    }

    private fun checkExtension(file: File, vararg extensions: String): Boolean {
        if (file.extension !in extensions) {
            println("Invalid file extension: $file. Must be one of: ${extensions.joinToString(", ")}.")
            return false
        }
        return true
    }
}
