package tri.ai.prompt.trace

/** Trace with references to info by index in database. */
data class AiPromptTraceId(
    var uuid: String,
    var promptIndex: Int,
    var modelIndex: Int,
    var execIndex: Int,
    var outputIndex: Int
)