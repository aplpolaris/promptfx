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
package tri.ai.core.agent.wf

/** 
 * Tracks intermediate results and other useful information. 
 * @deprecated Use ExecContext instead. WorkflowScratchpad is no longer used in WorkflowExecutor.
 */
@Deprecated("Use ExecContext instead", ReplaceWith("ExecContext", "tri.ai.core.tool.ExecContext"))
class WorkflowScratchpad {
    val data = mutableMapOf<String, WVar>()

    /** Add task results to the scratchpad. */
    fun addResults(task: WorkflowTask, outputs: List<WVar>) {
        outputs.map { WVar("${task.id}.${it.name}", it.description, it.value) }.forEach {
            data[it.name] = it
        }
    }
}
