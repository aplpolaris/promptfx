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
package tri.ai.prompt.run

import kotlinx.coroutines.runBlocking
import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.openai.jsonWriter
import tri.ai.openai.yamlWriter
import tri.ai.prompt.AiPrompt.Companion.fill
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.AiPromptModelInfo.Companion.MAX_TOKENS
import tri.ai.prompt.trace.AiPromptModelInfo.Companion.STOP
import tri.ai.prompt.trace.AiPromptModelInfo.Companion.TEMPERATURE
import tri.util.*
import java.io.File
import kotlin.system.exitProcess

object AiPromptRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val sampleArgs = arrayOf("D:\\data\\chatgpt\\prompt_batch_test.json",
            "D:\\data\\chatgpt\\prompt_batch_test_result.yaml", "--database")
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
            !checkExtension(outputFile, "json", "yaml", "yml"))
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
        val executor = RunnableExecutionPolicy()
        val result = runBlocking { executor.execute(batch) }
        println("${ANSI_CYAN}Processing complete.$ANSI_RESET")

        val writer = if (jsonOut) jsonWriter else yamlWriter
        val outputObject: Any = if (database) AiPromptTraceDatabase(result) else result
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

/**
 * Executes the series of prompt completions, using [TextPlugin].
 * Supports callbacks indicating when a completion starts and finishes.
 * @param batch the series of prompt and model configurations
 * @param initiated callback when a completion starts (blocks execution)
 * @param completed callback when a completion finishes (blocks execution)
 */
suspend fun RunnableExecutionPolicy.execute(
    batch: AiPromptBatch,
    initiated: (AiPromptRunConfig) -> Unit = { },
    completed: (AiPromptRunConfig, AiPromptTrace) -> Unit = { _, _ -> }
): List<AiPromptTrace> = batch.runConfigs().map {
    executeTextCompletion(it, it.second.model, initiated, completed)
}

/**
 * Execute a single prompt completion, using [TextPlugin].
 * Supports callbacks indicating when a completion starts and finishes.
 * @param runConfig the prompt and model configuration
 * @param modelId the model id to use for the completion
 * @param initiated callback when a completion starts (blocks execution)
 * @param completed callback when a completion finishes (blocks execution)
 */
suspend fun RunnableExecutionPolicy.executeTextCompletion(
    runConfig: AiPromptRunConfig,
    modelId: String,
    initiated: (AiPromptRunConfig) -> Unit = { },
    completed: (AiPromptRunConfig, AiPromptTrace) -> Unit = { _, _ -> }
): AiPromptTrace {
    info<AiPromptRunner>("Executing prompt with ${runConfig.second.model}: $ANSI_GRAY${runConfig.first.filled()}$ANSI_RESET")
    initiated(runConfig)
    val result = try {
        execute(runConfig, TextPlugin.textCompletionModel(modelId))
    } catch (x: NoSuchElementException) {
        AiPromptTrace(runConfig.first, runConfig.second, AiPromptExecInfo("Model not found: ${runConfig.second.model}"))
    }
    completed(runConfig, result)
    return result
}

/**
 * Executes a text completion with a single configuration.
 * Overwrites the model id in the configuration to match the model.
 * @param config the prompt and model configuration
 * @param completion the text completion model
 * @return trace of the execution, including output and run info
 */
suspend fun RunnableExecutionPolicy.execute(config: AiPromptRunConfig, completion: TextCompletion): AiPromptTrace {
    config.second.model = completion.modelId
    val promptText = config.first.filled()
    val result = execute {
        completion.complete(promptText, config.second)
    }
    return AiPromptTrace(config.first, config.second, AiPromptExecInfo(result.exception?.message), AiPromptOutputInfo(result.value?.value)).apply {
        execInfo.responseTimeMillis = result.duration.toMillis()
    }
}

suspend fun TextCompletion.complete(prompt: String, modelInfo: AiPromptModelInfo) =
    complete(prompt,
        tokens = modelInfo.modelParams[MAX_TOKENS] as? Int,
        temperature = modelInfo.modelParams[TEMPERATURE] as? Double,
        stop = modelInfo.modelParams[STOP] as? String
    )

fun AiPromptInfo.filled() = prompt.fill(promptParams)
