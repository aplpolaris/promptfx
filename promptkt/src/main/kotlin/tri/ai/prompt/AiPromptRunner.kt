package tri.ai.prompt

import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.prompt.AiPrompt.Companion.fill

/** Executes the series of prompt completions, using [TextPlugin]. */
suspend fun AiPromptRunSeries.execute() = runConfigs().map {
    val model = TextPlugin.textCompletionModels().firstOrNull { m -> m.modelId == it.model }
    model?.run(it) ?: AiPromptTrace.error(it, "Model not found: ${it.model}")
}

/**
 * Executes a text completion with a single configuration.
 * Overwrites the model id in the configuration to match the model.
 */
suspend fun TextCompletion.run(config: AiPromptConfig): AiPromptTrace {
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