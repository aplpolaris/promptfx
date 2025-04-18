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
package tri.ai.prompt.trace.batch

import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskMonitor
import tri.ai.prompt.trace.*

/** Configuration required for executing a text completion prompt. */
class AiPromptRunConfig(
    val promptInfo: AiPromptInfo,
    val modelInfo: AiModelInfo,
    val modelLookup: (String) -> TextCompletion = { TextPlugin.textCompletionModel(it) }
) {
    override fun toString() =
        "AiPromptRunConfig(promptInfo=$promptInfo, modelInfo=$modelInfo)"

    /** Create task for executing a run config. */
    fun task(id: String) = object : AiTask<String>(id) {
        override suspend fun execute(
            inputs: Map<String, AiPromptTraceSupport<*>>,
            monitor: AiTaskMonitor
        ): AiPromptTrace<String> = try {
            execute(modelLookup(modelInfo.modelId))
        } catch (x: NoSuchElementException) {
            AiPromptTrace(promptInfo, modelInfo, AiExecInfo.error("Model not found: ${modelInfo.modelId}"))
        }
    }

    /**
     * Executes a text completion with a single configuration.
     * Overwrites the model id in the configuration to match the model.
     * @param completion the text completion model
     * @return trace of the execution
     */
    private suspend fun execute(completion: TextCompletion): AiPromptTrace<String> {
        modelInfo.modelId = completion.modelId
        val promptText = promptInfo.filled()
        val result = completion.complete(promptText, modelInfo)
        return result.copy(promptInfo = promptInfo)
    }

    /**
     * Executes a single text completion query, with model parameters encoded in [modelInfo].
     */
    private suspend fun TextCompletion.complete(text: String, modelInfo: AiModelInfo) =
        complete(
            text = text,
            tokens = modelInfo.modelParams[AiModelInfo.MAX_TOKENS] as? Int,
            temperature = modelInfo.modelParams[AiModelInfo.TEMPERATURE] as? Double,
            stop = modelInfo.modelParams[AiModelInfo.STOP] as? String,
            numResponses = modelInfo.modelParams[AiModelInfo.NUM_RESPONSES] as? Int,
            history = listOf()
        )

}