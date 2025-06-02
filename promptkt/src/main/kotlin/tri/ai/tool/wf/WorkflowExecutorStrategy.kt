package tri.ai.tool.wf

/** Strategies for executing dynamic workflows. */
interface WorkflowExecutorStrategy {
    /**
     * Given the current state, including a task tree with tasks in various stages of completion,
     * determine an appropriate next step in the workflow, by specifically selecting a task and associated solver for that task.
     * @throws WorkflowToolNotFoundException
     * @throws WorkflowTaskNotFoundException
     */
    suspend fun nextSolver(state: WorkflowState, solvers: List<WorkflowSolver>): Pair<WorkflowSolver, WorkflowTask>
    /**
     * Identify a task checker to either break up a task, or determine that the initial workflow tasking has been solved.
     */
    suspend fun decomposeTask(state: WorkflowState, solvers: List<WorkflowSolver>): WorkflowTaskPlan
}