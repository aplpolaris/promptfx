package tri.ai.tool.wf

/** A task that can be "solved" within the workflow. */
abstract class WorkflowTask(
    val id: String,
    val name: String,
    val description: String,
    var isDone: Boolean = false
)

/** Special task representing the entire problem-solving process. */
class WorkflowSolveTask : WorkflowTask("solve", "Solver", "Solves the overall workflow") {
    override fun toString() = "WorkflowSolveTask"
}

/** Special task representing a user request. */
class WorkflowUserRequest(val request: String) : WorkflowTask("user-request", "User Request", "The user's request") {
    override fun toString() = "WorkflowUserRequest(id='$id', name='$name', description='$description', request='$request')"
}

/** Special task representing a validation step confirming a user request has been answered. */
class WorkflowValidatorTask :
    WorkflowTask(TASK_ID, "Validator", "Validates that a final result answers the user's request") {
    override fun toString() = "WorkflowValidatorTask"
    companion object {
        const val TASK_ID = "validator"
    }
}

/** General task with just a name and description. */
class WorkflowTaskTool(
    id: String,
    name: String,
    description: String = "",
    isDone: Boolean = false,
    val tool: String = "",
    val inputs: List<String>
) : WorkflowTask(id, name, description, isDone) {
    override fun toString() =
        "WorkflowTaskTool(id='$id', name='$name', description='$description', isDone=$isDone, tool='$tool', inputs=$inputs)"
}