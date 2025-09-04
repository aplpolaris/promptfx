/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.util.ui.starship

import javafx.scene.image.Image
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.fill
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.PromptInfo
import tri.ai.prompt.trace.PromptInfo.Companion.filled
import tri.ai.prompt.trace.batch.AiPromptRunConfig
import tri.ai.text.docs.FormattedText
import tri.promptfx.PromptFxGlobals.lookupPrompt
import tri.util.ui.DocumentThumbnail
import tri.ai.pips.core.ExecContext
import com.fasterxml.jackson.databind.JsonNode
import tri.ai.pips.core.MAPPER

/** Pipeline execution for [StarshipUi]. */
object StarshipPipeline {

    /** Execute a pipeline using the new JSON-configurable approach. */
    fun execWithJsonPipeline(config: StarshipPipelineConfig, results: StarshipPipelineResults, delayMillis: Long = 0L) {
        results.started.set(true)
        results.activeStep.set(0)
        results.activeStep.set(1)

        val input = config.generator()
        results.input.set(input)

        // Create execution context with user input
        val context = ExecContext()
        context.vars["userInput"] = MAPPER.valueToTree(input)

        // Create a basic run config for UI display purposes
        val runConfig = AiPromptRunConfig(
            PromptInfo(config.primaryPrompt.prompt.template!!, mapOf(PromptTemplate.INPUT to input) + config.primaryPrompt.params),
            AiModelInfo(config.chatEngine.modelId)
        )
        results.runConfig.set(runConfig)

        runBlocking {
            delay(delayMillis)
            results.activeStep.set(2)
            
            // Execute the JSON pipeline
            config.pipelineExecutor.execute(config.pipeline, context)
            
            delay(delayMillis)
            results.activeStep.set(3)

            // Extract primary output from pipeline results
            context.vars["primaryOutput"]?.let { primaryOutputNode ->
                val primaryText = primaryOutputNode.get("text")?.asText() ?: primaryOutputNode.toString()
                val primaryResult = StarshipInterimResult(
                    "Primary Analysis",
                    FormattedText(primaryText),
                    null,
                    listOf()
                )
                results.output.set(primaryResult)
            }

            // Extract secondary outputs
            val secondarySteps = listOf("simplifiedOutput", "outlineOutput", "technicalTermsOutput", "translatedOutput")
            secondarySteps.forEachIndexed { index, stepKey ->
                context.vars[stepKey]?.let { outputNode ->
                    val outputText = outputNode.get("text")?.asText() ?: outputNode.toString()
                    val secondaryResult = StarshipInterimResult(
                        stepKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        FormattedText(outputText),
                        null,
                        listOf()
                    )
                    
                    delay(delayMillis)
                    runLater {
                        if (index == 0) {
                            results.activeStep.set(4)
                            results.outputHighlight.set(secondaryResult)
                        } else {
                            results.activeStep.set(6)
                            results.activeStep.set(5)
                            results.secondaryOutputs.add(secondaryResult)
                        }
                    }
                }
            }
        }

        results.completed.set(true)
    }

    /** Execute a pipeline, adding interim results to the [results] object. */
    fun exec(config: StarshipPipelineConfig, results: StarshipPipelineResults, delayMillis: Long = 0L) {
        // Use the new JSON pipeline by default
        execWithJsonPipeline(config, results, delayMillis)
    }

    /** Legacy execution method for backward compatibility. */
    fun execLegacy(config: StarshipPipelineConfig, results: StarshipPipelineResults, delayMillis: Long = 0L) {
        results.started.set(true)
        results.activeStep.set(0)
        results.activeStep.set(1)

        val input = config.generator()
        results.input.set(input)

        val runConfig = AiPromptRunConfig(
            PromptInfo(config.primaryPrompt.prompt.template!!, mapOf(PromptTemplate.INPUT to input) + config.primaryPrompt.params),
            AiModelInfo(config.chatEngine.modelId)
        )
        results.runConfig.set(runConfig)

        runBlocking { delay(delayMillis) }
        results.activeStep.set(2)
        val firstResponse = runBlocking {
            config.promptExec.exec(config.primaryPrompt, runConfig.promptInfo.filled())
        }
        runBlocking { delay(delayMillis) }
        results.activeStep.set(3)
        results.output.set(firstResponse)

        config.secondaryPrompts.forEach {
            val secondInput = results.output.value.rawText
            val secondRunConfig = AiPromptRunConfig(
                PromptInfo(it.prompt.template!!, mapOf(PromptTemplate.INPUT to secondInput) + it.params),
                AiModelInfo(config.chatEngine.modelId)
            )
            results.secondaryRunConfigs.add(secondRunConfig)
        }

        runBlocking { delay(delayMillis) }
        results.secondaryRunConfigs.forEachIndexed { i, cfg ->
            val prompt = config.secondaryPrompts[i]
            val secondResponse = runBlocking {
                config.secondaryPromptExec.exec(prompt, cfg.promptInfo.filled())
            }
            runBlocking { delay(delayMillis) }
            runLater {
                if (i == 0) {
                    results.activeStep.set(4)
                    results.outputHighlight.set(secondResponse)
                } else {
                    results.activeStep.set(6)
                    results.activeStep.set(5)
                    results.secondaryOutputs.add(secondResponse)
                }
            }
        }

        results.completed.set(true)
    }

}

/** Executes a prompt, returning a string and optional image. */
interface AiPromptExecutor {
    suspend fun exec(prompt: PromptWithParams, input: String): StarshipInterimResult
}

/** Result object for [StarshipPipeline]. */
class StarshipInterimResult(val label: String, val text: FormattedText, val image: Image?, val docs: List<DocumentThumbnail>) {
    val rawText
        get() = text.toString()
}

/** Groups a prompt with associated parameters. */
class PromptWithParams(val prompt: PromptDef, val params: MutableMap<String, Any> = mutableMapOf()) {
    constructor(promptId: String, params: Map<String, Any> = mapOf()) :
            this(lookupPrompt(promptId), params.toMutableMap())

    fun fill(input: String) = prompt.fill(mapOf(PromptTemplate.INPUT to input) + params)
}