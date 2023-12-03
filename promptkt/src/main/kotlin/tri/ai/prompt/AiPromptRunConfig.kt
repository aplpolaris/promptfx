package tri.ai.prompt

import tri.ai.core.TextCompletion
import tri.ai.prompt.AiPrompt.Companion.fill

/** Configuration for running a single prompt completion. */
data class AiPromptRunConfig(
    var model: String? = null,
    var modelParams: Map<String, Any> = mapOf(),
    var prompt: String = "",
    var promptParams: Map<String, Any> = mapOf()
)

suspend fun TextCompletion.run(config: AiPromptRunConfig): AiPromptTrace {
    return try {
        val t0 = System.currentTimeMillis()
        config.model = modelId
        val filled = config.prompt.fill(config.promptParams)
        val output = complete(filled,
            config.modelParams["maxTokens"] as? Int,
            config.modelParams["temperature"] as? Double,
            config.modelParams["stop"] as? String
        )
        val completionTime = System.currentTimeMillis() - t0
        if (output.error != null) {
            AiPromptTrace.error(config, output.error.message)
        } else {
            AiPromptTrace(config, output.value)
        }.apply {
            responseTimeMillis = completionTime
        }
    } catch (x: Exception) {
        AiPromptTrace.error(config, x.message)
    }
}

