package tri.ai.pips

/** Result of a pipeline execution. */
class AiPipelineResult(finalTaskId: String, val results: Map<String, AiTaskResult<*>>) {

    /** The result of the last task in the pipeline. */
    val finalResult: Any? = results[finalTaskId]?.value?.let {
        when (it) {
            is AiPipelineResult -> it.finalResult
            is AiTaskResult<*> -> it.value
            else -> it
        }
    }

    companion object {
        fun error(message: String, error: Throwable) = AiPipelineResult(
            "error",
            mapOf("error" to AiTaskResult.error(message, error))
        )

        fun todo() = error("This pipeline is not yet implemented.", UnsupportedOperationException())
    }

}