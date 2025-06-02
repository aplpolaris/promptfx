package tri.ai.tool.wf

/** Solver that takes a single input, provides a single output, based on a runner. */
class RunSolver(
    name: String,
    description: String,
    inputDescription: String,
    outputDescription: String,
    val run: (String) -> String
) : WorkflowSolver(name, description, mapOf("input" to inputDescription), mapOf("result" to outputDescription)) {
    override suspend fun solve(
        state: WorkflowState,
        task: WorkflowTask
    ): WorkflowSolveStep {
        val t0 = System.currentTimeMillis()
        val inputData = state.aggregateInputsFor(name).values.joinToString("\n") { "${it?.value}" }
        val result = run(inputData)
        return solveStep(
            task,
            inputs(inputData),
            outputs(result),
            System.currentTimeMillis() - t0,
            true
        )
    }

}