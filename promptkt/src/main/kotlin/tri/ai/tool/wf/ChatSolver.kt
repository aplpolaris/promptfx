package tri.ai.tool.wf

import tri.ai.openai.OpenAiCompletionChat
import tri.ai.prompt.AiPromptLibrary

val PROMPTS = AiPromptLibrary.Companion.readResource<WorkflowExecutor>()

/** Solver that takes a single input, provides a single output, based on an LLM chat request. */
class ChatSolver(
    name: String,
    description: String,
    inputDescription: String,
    outputDescription: String,
    val promptId: String
) : WorkflowSolver(name, description, mapOf("input" to inputDescription), mapOf("result" to outputDescription)) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val inputs = state.aggregateInputsFor(name).values.mapNotNull { it?.value }.ifEmpty {
            listOf(task.name)
        }
        val inputData = inputs.joinToString("\n")

        val prompt = PROMPTS.fill(promptId, "input" to inputData)
        val result = OpenAiCompletionChat().complete(prompt, tokens = 1000)

        return solveStep(
            task,
            inputs(inputData),
            outputs(result.firstValue),
            result.exec.responseTimeMillisTotal ?: 0L,
            true
        )
    }

}

