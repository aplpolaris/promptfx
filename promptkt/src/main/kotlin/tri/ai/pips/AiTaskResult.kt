package tri.ai.pips

/** Result of executing a task. */
data class AiTaskResult<T>(val value: T? = null, val error: Throwable? = null, val modelId: String?) {

    fun <S> map(function: (T) -> S) = AiTaskResult(value?.let { function(value) }, error, modelId)

    /** Wraps this as a pipeline result. */
    fun asPipelineResult() = AiPipelineResult("result", mapOf("result" to this))

    companion object {
        /** Task with token result. */
        fun <T> result(value: T, modelId: String? = null) =
            AiTaskResult(value, null, modelId)

        /** Task not attempted because input was invalid. */
        fun invalidRequest(message: String) =
            AiTaskResult(message, IllegalArgumentException(message), null)

        /** Task not attempted or successful because of a general error. */
        fun error(message: String, error: Throwable) =
            AiTaskResult(message, error, null)
    }

}