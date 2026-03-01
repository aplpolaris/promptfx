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

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.CompletionBuilder
import tri.ai.core.TextPlugin
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.impl.PROMPTS
import tri.ai.prompt.fill
import tri.util.json.createJsonSchema
import tri.util.json.createObject
import tri.util.json.jsonMapper
import tri.util.json.yamlMapper
import java.io.IOException

/** A solver used to validate that a computed result answers the actual user question. */
class FinalValiditySolver(val config: AgentChatConfig) : WorkflowSolver(
    "Validate Final Result",
    "Checks if the final result answers the user's request",
    "",
    createJsonSchema(REQUEST to "User's initial request", RESULT to "Workflow's final result"),
    createJsonSchema(ANSWERED to "Flag indicating whether request has been answered", RATIONALE to "Assessment of result validity/relevance", VALIDATED_RESULT to "Validated final result")
) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val t0 = System.currentTimeMillis()

        // get input information from the state
        val userRequest = state.request.request
        val finalTaskId = state.taskTree.findTask { it is WorkflowUserRequest }!!.tasks.last().root.id
        val finalResult = state.scratchpad.vars["$finalTaskId.result"]!!
        val finalResultText = finalResult.prettyPrint()

        // complete a prompt doing the assessment
        val prompt = PROMPTS.get(VALIDATOR_PROMPT_ID)!!.fill(
            USER_REQUEST_PARAM to userRequest,
            PROPOSED_RESULT_PARAM to finalResultText
        )

        // use LLM to generate a response
        val chat = TextPlugin.Companion.chatModel(config.modelId)
        val response = CompletionBuilder()
            .tokens(config.maxTokens)
            .temperature(config.temperature)
            .text(prompt)
            .execute(chat)

        // parse the response and use it to build a set of subtasks to solve
        val validity = parseValidity(response.firstValue.textContent())

        // return a result object
        val t1 = System.currentTimeMillis()
        return WorkflowSolveStep(
            task,
            this,
            createObject(REQUEST to userRequest, RESULT to finalResultText),
            jsonMapper.createObjectNode().apply {
                put(ANSWERED, validity.isRequestAnswered)
                put(RATIONALE, validity.rationale)
                put(VALIDATED_RESULT, finalResultText)
            },
            t1 - t0,
            true
        )
    }

    private fun parseValidity(value: String): ResultValidity {
        val quotedResponse = value.findCode()
        // parse into yaml and return
        return try {
            yamlMapper.readValue<ResultValidity>(quotedResponse)
        } catch (e: IOException) {
            error("Failed to parse response: $e\n$quotedResponse\n$value")
        }
    }
}

data class ResultValidity(
    val isRequestAnswered: Boolean,
    val rationale: String
)
