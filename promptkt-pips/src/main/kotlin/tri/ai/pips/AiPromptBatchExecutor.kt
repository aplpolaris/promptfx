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
package tri.ai.pips

import tri.ai.core.TextChat
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.ai.prompt.trace.batch.AiPromptBatch
import tri.ai.prompt.trace.batch.AiPromptRunConfig

/**
 * Generate executable list of tasks for a prompt batch.
 * These can be passed to [AiPipelineExecutor] for execution.
 */
fun AiPromptBatch.tasks(modelLookup: (String) -> TextChat): List<AiTask<String>> =
    runConfigs(modelLookup).mapIndexed { i, v -> v.task("$id $i") }

/** Get an [AiPlanner] for executing this batch of prompts. */
fun AiPromptBatch.plan(modelLookup: (String) -> TextChat) =
    tasks(modelLookup).aggregate().planner

/** Create task for executing a run config. */
fun AiPromptRunConfig.task(id: String) = object : AiTask<String>(id) {
    override suspend fun execute(
        inputs: Map<String, AiPromptTraceSupport<*>>,
        monitor: AiTaskMonitor
    ): AiPromptTrace<String> = try {
        execute(modelLookup(modelInfo.modelId))
    } catch (x: NoSuchElementException) {
        AiPromptTrace(promptInfo, modelInfo, AiExecInfo.error("Model not found: ${modelInfo.modelId}"))
    }
}
