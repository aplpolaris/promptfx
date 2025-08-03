package tri.ai.tool.wf

import tri.ai.core.TextCompletion
import tri.ai.openai.OpenAiCompletionChat

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
        val prompt = PROMPTS.fill(AGGREGATOR_PROMPT_ID,
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