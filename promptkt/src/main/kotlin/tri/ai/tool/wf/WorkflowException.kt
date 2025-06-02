package tri.ai.tool.wf

/** General exception within dynamic workflow execution. */
open class WorkflowException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Workflow exception caused by tool not available/not found. */
class WorkflowToolNotFoundException(message: String, cause: Throwable? = null) : WorkflowException(message, cause)

/** Workflow exception caused by task not available/not found. */
class WorkflowTaskNotFoundException(message: String, cause: Throwable? = null) : WorkflowException(message, cause)