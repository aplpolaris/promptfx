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

import tri.ai.core.TextCompletion
import tri.ai.openai.OpenAiCompletionChat
import tri.ai.prompt.fill

/** A solver used to aggregate/finalize a response for an original user question. */
class WAggregatorSolver(val completionEngine: TextCompletion, val maxTokens: Int, val temp: Double) : WorkflowSolver(
    "Aggregator",
    "Combines results from multiple tasks to produce a final answer.",
    mapOf(REQUEST to "User's initial request", INTERMEDIATE_RESULTS to "Workflow's intermediate results"),
    mapOf(RESULT to "Final aggregated result")
) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val userRequest = state.request.request
        val inputs = state.aggregateInputsFor(name).values.mapNotNull { it?.value }.ifEmpty {
            listOf(task.name)
        }
        val inputData = inputs.joinToString("\n")
        val prompt = PROMPTS.get(AGGREGATOR_PROMPT_ID)!!.fill(
            USER_REQUEST_PARAM to userRequest,
            INPUTS_PARAM to inputData
        )
        val result = OpenAiCompletionChat().complete(prompt, tokens = 1000)
        // find answer between <<< and >>> if they exist
        val quotedResult = result.firstValue.findCode()

        return solveStep(
            task,
            inputs(userRequest, inputData),
            outputs(quotedResult),
            result.exec.responseTimeMillisTotal ?: 0L,
            true
        )
    }
}
