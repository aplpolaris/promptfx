package tri.ai.tool.wf

import com.fasterxml.jackson.module.kotlin.readValue
import okio.IOException
import tri.ai.core.MAPPER
import tri.ai.core.TextCompletion

/** A solver used to validate that a computed result answers the actual user question. */
class WValiditySolver(val completionEngine: TextCompletion, val maxTokens: Int, val temp: Double) : WorkflowSolver(
    "Validate Final Result",
    "",
    mapOf("request" to "User's initial request", "result" to "Workflow's final result"),
    mapOf("answered" to "Flag indicating whether request has been answered", "rationale" to "Assessment of result validity/relevance", VALIDATED_RESULT to "Validated final result")
) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val t0 = System.currentTimeMillis()

        // get input information from the state
        val userRequest = state.request.request
        val finalTaskId = state.taskTree.findTask { it is WorkflowUserRequest }!!.tasks.last().root.id
        val finalResult = state.scratchpad.data["$finalTaskId.result"]!!

        // complete a prompt doing the assessment
        val prompt = PROMPTS.fill("tool-validate",
            "user_request" to userRequest,
            "proposed_result" to finalResult.value.toString()
        )

        // use LLM to generate a response
        val response = completionEngine.complete(prompt, tokens = maxTokens, temperature = temp)

        // parse the response and use it to build a set of subtasks to solve
        val validity = parseValidity(response.firstValue)

        // return a result object
        val t1 = System.currentTimeMillis()
        return WorkflowSolveStep(
            task,
            this,
            inputs(userRequest, finalResult.value),
            outputs(validity.isRequestAnswered, validity.rationale, finalResult.value),
            t1 - t0,
            true
        )
    }

    private fun parseValidity(value: String): ResultValidity {
        val quotedResponse = value.findCode()
        // parse into yaml and return
        return try {
            MAPPER.readValue<ResultValidity>(quotedResponse)
        } catch (e: IOException) {
            error("Failed to parse response: $e\n$quotedResponse")
        }
    }

    companion object {
        private const val VALIDATED_RESULT = "validated_result"
        val finalResultId = "${WorkflowValidatorTask.TASK_ID}.$VALIDATED_RESULT"
    }
}

data class ResultValidity(
    val isRequestAnswered: Boolean,
    val rationale: String
)
