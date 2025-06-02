package tri.ai.tool.wf

/** Workflow task tree. */
class WorkflowTaskTree(
    /** Parent task. */
    val root: WorkflowTask,
    /** Child tasks that should be completed first. */
    _tasks: List<WorkflowTaskTree> = listOf()
) {

    /** Child tasks that should be completed first. */
    val tasks: MutableList<WorkflowTaskTree> = _tasks.toMutableList()

    /** Flag indicating if the task tree is complete. */
    var isDone: Boolean = false
        set(value) {
            if (value) {
                require(tasks.all { it.isDone })
            }
            field = value
            root.isDone = value
        }

    override fun toString(): String {
        return "WorkflowTaskTree(root=$root, tasks=$tasks)"
    }

    /** Find first task in tree matching given predicate. */
    fun findTask(predicate: (WorkflowTask) -> Boolean): WorkflowTaskTree? {
        if (predicate(root)) {
            return this
        }
        for (task in tasks) {
            task.findTask(predicate)?.let { return it }
        }
        return null
    }

    /** Update done status within the hierarchy, percolating subtask completion up the chain. */
    fun setTaskDone(task: WorkflowTask) {
        if (root == task) {
            isDone = true
            task.isDone = true
            return
        } else {
            tasks.forEach { it.setTaskDone(task) }
            if (tasks.isNotEmpty() && tasks.all { it.isDone }) {
                isDone = true
                root.isDone = true
            }
        }
    }

    companion object {
        /** Creates a solve tree with given set of subtasks. */
        fun createFromSubtasks(root: WorkflowTask, vararg subtasks: WorkflowTask) =
            WorkflowTaskTree(root, subtasks.map { WorkflowTaskTree(it) })

        /** Creates a solve tree with steps for solving the user question and validating the question. */
        fun createSolveAndValidateTree(problem: WorkflowUserRequest): WorkflowTaskTree {
            val root = WorkflowSolveTask()
            val problemTask = problem
            val validateTask = WorkflowValidatorTask()
            return createFromSubtasks(root, problemTask, validateTask)
        }
    }
}