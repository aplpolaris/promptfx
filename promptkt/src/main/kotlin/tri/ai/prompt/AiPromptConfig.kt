package tri.ai.prompt

/** Configuration for running a single prompt completion. */
data class AiPromptConfig(
    var model: String? = null,
    var modelParams: Map<String, Any> = mapOf(),
    var prompt: String = "",
    var promptParams: Map<String, Any> = mapOf()
)
