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

/** Takes user input and generates a series of tasks to be executed. */
interface AiPlanner {

    fun plan(): List<AiTask<*>>

    /** Executes the plan with [AiPipelineExecutor]. */
    suspend fun execute(monitor: AiTaskMonitor) = AiPipelineExecutor.execute(plan(), monitor)

    companion object {
        /** Consolidates all planners into a single planner. */
        fun batchPlan(planners: List<AiTaskList<String>>) = object : AiPlanner {
            override fun plan() = planners.flatMap { it.plan }
        }
    }

}
