/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.util.json.jsonMapper
import tri.util.json.tryJson
import kotlin.collections.ifEmpty

/** Key for storing [WorkflowPlanState] in the context via [ExecContext.putResource]. */
const val RESOURCE_WORKFLOW_PLAN_STATE = "workflowPlanState"
/** Key for storing the current [WorkflowTask] in the context via [ExecContext.putResource]. */
const val RESOURCE_WORKFLOW_TASK = "currentWorkflowTask"

/** Extension property to get [WorkflowPlanState] from the context. */
val ExecContext.workflowPlanState: WorkflowPlanState
    get() = resource(RESOURCE_WORKFLOW_PLAN_STATE) as WorkflowPlanState

/** Extension property to get the current [WorkflowTask] from the context. */
val ExecContext.currentWorkflowTask: WorkflowTask
    get() = resource(RESOURCE_WORKFLOW_TASK) as WorkflowTask

/**
 * Initializes the context scratchpad with the user input extracted from the workflow request.
 * Must be called after [RESOURCE_WORKFLOW_PLAN_STATE] has been stored via [ExecContext.putResource].
 */
fun ExecContext.initWorkflowContext() {
    val request = workflowPlanState.request
    // TODO - this is very brittle
    val userInput = request.request.substringAfter("\n").trim().substringAfter("\"\"\"").substringBefore("\"\"\"").trim()
    if (userInput.isNotEmpty())
        put("user_input", jsonMapper.valueToTree<JsonNode>(userInput))
}

/** Writes task outputs into the context scratchpad, keyed as "{taskId}.{outputKey}". */
fun ExecContext.addWorkflowResults(task: WorkflowTask, outputs: ObjectNode) {
    outputs.properties().forEach { (key, value) ->
        put("${task.id}.$key", value)
    }
}

/** Using the current plan, looks up all scratchpad inputs associated with the given tool. */
fun ExecContext.aggregateWorkflowInputsFor(toolName: String): Map<String, JsonNode> {
    val state = workflowPlanState
    return ((state.taskTree.findTask { it is WorkflowTaskTool && it.tool == toolName }?.root as? WorkflowTaskTool)?.inputs
        ?: listOf()).associateWith { getJson("$it.$RESULT") ?: getJson(it)!! }
}

/** Aggregates the scratchpad inputs for the given tool as a single string value. */
fun ExecContext.aggregateWorkflowInputsAsStringFor(toolName: String, taskName: String, separator: String = "\n"): String =
    aggregateWorkflowInputsFor(toolName).values.mapNotNull { it.workflowPrettyPrint() }.ifEmpty { listOf(taskName) }.joinToString(separator)

/** Get the final computed result from the context scratchpad. */
// TODO - this is brittle since it assumes a specific output exists in the scratchpad
fun ExecContext.workflowFinalResult(): JsonNode =
    getJson(FINAL_RESULT_ID)!!

private fun JsonNode.workflowPrettyPrint() = when {
    isTextual -> asText()
    else -> toString()
}

/** Advances workflow towards a solution. */
abstract class WorkflowSolver(
    override val name: String,
    override val description: String,
    override val version: String,
    override val inputSchema: JsonNode,
    override val outputSchema: JsonNode
): Executable {
    constructor(name: String, description: String, version: String, inputSchema: String, outputSchema: String):
            this(name, description, version, inputSchema.tryJson()!!, outputSchema.tryJson()!!)

    /** Provide a plaintext description of this solver for use in prompts. */
    fun descriptionForPrompts() = "$name: $description"

    /**
     * Executes this solver step using the provided context.
     * The [WorkflowPlanState] and current [WorkflowTask] are available via [ExecContext.workflowPlanState]
     * and [ExecContext.currentWorkflowTask] respectively.
     * Returns the output as a [JsonNode] (typically an [ObjectNode]).
     */
    abstract override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode
}

/** Logs a step taken during workflow execution. */
data class WorkflowSolveStep(
    val task: WorkflowTask,
    val solver: WorkflowSolver,
    val inputs: ObjectNode,
    val outputs: ObjectNode,
    val executionTimeMillis: Long,
    val isSuccess: Boolean
)
