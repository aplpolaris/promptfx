/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.util.ui.starship

import javafx.scene.image.Image
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPrompt.Companion.INPUT
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.batch.AiPromptRunConfig
import tri.promptfx.ui.FormattedText
import tri.util.ui.DocumentThumbnail

/** Pipeline execution for [StarshipUi]. */
object StarshipPipeline {

    /** Execute a pipeline, adding interim results to the [results] object. */
    fun exec(config: StarshipPipelineConfig, results: StarshipPipelineResults, delayMillis: Long = 0L) {
        results.started.set(true)
        results.activeStep.set(0)
        results.activeStep.set(1)

        val input = config.generator()
        results.input.set(input)

        val runConfig = AiPromptRunConfig(
            AiPromptInfo(config.primaryPrompt.prompt.template, mapOf(INPUT to input) + config.primaryPrompt.params),
            AiModelInfo(config.completion.modelId)
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
                AiPromptInfo(it.prompt.template, mapOf(INPUT to secondInput) + it.params),
                AiModelInfo(config.completion.modelId)
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
class PromptWithParams(val prompt: AiPrompt, val params: MutableMap<String, Any> = mutableMapOf()) {
    constructor(prompt: String, params: Map<String, Any> = mapOf()) :
            this(AiPromptLibrary.lookupPrompt(prompt), params.toMutableMap())

    fun fill(input: String) = prompt.fill(mapOf(INPUT to input) + params)
}