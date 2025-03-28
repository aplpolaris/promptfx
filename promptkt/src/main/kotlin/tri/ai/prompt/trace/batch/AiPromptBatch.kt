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
import tri.ai.pips.*

/** Provides a series of prompt/model pairings for execution. */
abstract class AiPromptBatch(val id: String) {

    /** Get all run configs within this series. */
    abstract fun runConfigs(modelLookup: (String) -> TextCompletion): Iterable<AiPromptRunConfig>

    /**
     * Generate executable list of tasks for a prompt batch.
     * These can be passed to [AiPipelineExecutor] for execution.
     */
    fun tasks(modelLookup: (String) -> TextCompletion): List<AiTask<String>> =
        runConfigs(modelLookup).mapIndexed { i, v -> v.task("$id $i") }

    /** Get an [AiPlanner] for executing this batch of prompts. */
    fun plan(modelLookup: (String) -> TextCompletion) = tasks(modelLookup).aggregate().planner
}