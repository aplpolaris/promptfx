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

import tri.ai.core.CompletionBuilder
import tri.ai.core.TextPlugin
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.impl.PROMPTS
import tri.ai.prompt.fill
import tri.util.json.createJsonSchema
import tri.util.json.createObject

/** A solver used to aggregate/finalize a response for an original user question. */
class FinalAggregatorSolver(val config: AgentChatConfig) : WorkflowSolver(
    "Aggregator",
    "Combines results from multiple tasks to produce a final answer.",
    "",
    createJsonSchema(REQUEST to "User's initial request", INTERMEDIATE_RESULTS to "Workflow's intermediate results"),
    createJsonSchema(RESULT to "Final aggregated result")
) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val userRequest = state.request.request
        val inputData = state.aggregateInputsAsStringFor(name, task.name)
        val prompt = PROMPTS.get(AGGREGATOR_PROMPT_ID)!!.fill(
            USER_REQUEST_PARAM to userRequest,
            INPUTS_PARAM to inputData
        )

        val chat = TextPlugin.Companion.chatModel(config.modelId)
        val response = CompletionBuilder()
            .tokens(config.maxTokens)
            .temperature(config.temperature)
            .text(prompt)
            .execute(chat)

        // find answer between <<< and >>> if they exist
        val quotedResult = response.firstValue.textContent().findCode()

        return WorkflowSolveStep(
            task,
            this,
            createObject(REQUEST to userRequest, INTERMEDIATE_RESULTS to inputData),
            createObject(RESULT to quotedResult),
            response.exec.responseTimeMillisTotal ?: 0L,
            true
        )
    }
}
