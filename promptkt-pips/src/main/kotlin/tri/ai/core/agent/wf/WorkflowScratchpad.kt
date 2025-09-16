package tri.ai.core.agent.wf

/** Tracks intermediate results and other useful information. */
class WorkflowScratchpad {
    val data = mutableMapOf<String, WVar>()

    /** Add task results to the scratchpad. */
    fun addResults(task: WorkflowTask, outputs: List<WVar>) {
        outputs.map { WVar("${task.id}.${it.name}", it.description, it.value) }.forEach {
            data[it.name] = it
        }
    }
}