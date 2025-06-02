package tri.ai.tool.wf

/** Strategy for decomposing tasks within a workflow. */
interface WorkflowTaskDecomposer {
    /** Decomposes pending tasks in the workflow. */
    fun decompose(state: WorkflowState): WorkflowTaskPlan
}

/** Results of a workflow planning step, consisting of a list of decomposed tasks. */
class WorkflowTaskPlan(
    /** New task decompositions resulting from the planning step. */
    val decomp: List<WorkflowTaskTree>
) {
    /** Returns a string representation of the task plan. */
    override fun toString() = decomp.joinToString("\n") { it.toString() }
}