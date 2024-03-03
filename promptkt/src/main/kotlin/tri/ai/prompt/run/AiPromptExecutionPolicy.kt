package tri.ai.prompt.run

/** Policy for re-attempting failed prompt completions. */
class AiPromptExecutionPolicy(
    var maxRetries: Int = 3,
    var retryDelay: Long = 1000L
)