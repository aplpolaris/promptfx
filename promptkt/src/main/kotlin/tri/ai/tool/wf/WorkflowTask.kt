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
package tri.ai.tool.wf

/** A task that can be "solved" within the workflow. */
abstract class WorkflowTask(
    val id: String,
    val name: String,
    val description: String,
    var isDone: Boolean = false
)

/** Special task representing the entire problem-solving process. */
class WorkflowSolveTask : WorkflowTask("solve", "Solver", "Solves the overall workflow") {
    override fun toString() = "WorkflowSolveTask"
}

/** Special task representing a user request. */
class WorkflowUserRequest(val request: String) : WorkflowTask("user-request", "User Request", "The user's request") {
    override fun toString() = "WorkflowUserRequest(id='$id', name='$name', description='$description', request='$request')"
}

/** Special task representing a validation step confirming a user request has been answered. */
class WorkflowValidatorTask :
    WorkflowTask(TASK_ID, "Validator", "Validates that a final result answers the user's request") {
    override fun toString() = "WorkflowValidatorTask"
    companion object {
        const val TASK_ID = "validator"
    }
}

/** General task with just a name and description. */
class WorkflowTaskTool(
    id: String,
    name: String,
    description: String = "",
    isDone: Boolean = false,
    val tool: String = "",
    val inputs: List<String>
) : WorkflowTask(id, name, description, isDone) {
    override fun toString() =
        "WorkflowTaskTool(id='$id', name='$name', description='$description', isDone=$isDone, tool='$tool', inputs=$inputs)"
}
