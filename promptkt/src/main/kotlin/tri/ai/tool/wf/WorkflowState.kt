package tri.ai.tool.wf

import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET

/** Tracks state of workflow execution. */
class WorkflowState(_request: WorkflowUserRequest) {
    /** The user request that needs to be solved. */
    var request = _request
    /** Tree of tasks required for workflow execution. */
    val taskTree = WorkflowTaskTree.createSolveAndValidateTree(request)
    /** Scratchpad containing intermediate results and other useful information. */
    val scratchpad = WorkflowScratchpad()
    /** History of solver steps taken. */
    val solveHistory = mutableListOf<WorkflowSolveStep>()
    /** Flag indicating when the execution is complete. */
    var isDone: Boolean = false
        private set

    /** Check for completion, updating the isDone flag if all tasks are complete. */
    fun checkDone() {
        isDone = taskTree.isDone
    }

    /** Updates task tree based on a planning result. */
    fun updateTasking(plan: WorkflowTaskPlan) {
        plan.decomp.forEach { newNode ->
            val curNode = taskTree.findTask { it == newNode.root } ?: error("Task not found: ${newNode.root}")
            curNode.tasks.clear()
            curNode.tasks.addAll(newNode.tasks)
            curNode.isDone = newNode.isDone
        }
        printTaskPlan(plan.decomp)
    }

    internal fun printTaskPlan(trees: List<WorkflowTaskTree>, indent: String = "") {
        trees.forEach { tree ->
            print(ANSI_GRAY)
            print(indent)
            if ((tree.root as? WorkflowTaskTool)?.id != null)
                print("[${tree.root.id}] ")
            print(tree.root.name)
            if (tree.root.description.isNotBlank())
                print(": ${tree.root.description}")
            print(", Completed: ${tree.root.isDone}")
            if ((tree.root as? WorkflowTaskTool)?.inputs.let { it != null && !it.isEmpty() })
                print(", Inputs: ${(tree.root as WorkflowTaskTool).inputs}")
            println("")
            printTaskPlan(tree.tasks, indent = indent.removeSuffix("- ") + "  - ")
        }
        print(ANSI_RESET)
    }

    /** Using the current plan, look up all the inputs associated with the given tool. */
    fun aggregateInputsFor(toolName: String) =
        ((taskTree.findTask { it is WorkflowTaskTool && it.tool == toolName }?.root as? WorkflowTaskTool)?.inputs
            ?: listOf()).associateWith { scratchpad.data["$it.result"] }

    /** Get the final computed result from the scratchpad. */
    fun finalResult() =
        // TODO - this is brittle since it assumes a specific output exists on the scratchpad, want a general purpose solution
        scratchpad.data[WValiditySolver.finalResultId]!!.value
}

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