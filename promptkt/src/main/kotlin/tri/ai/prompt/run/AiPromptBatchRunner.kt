package tri.ai.prompt.run

import tri.ai.core.TextPlugin
import tri.ai.prompt.trace.AiPromptExecInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Executes the series of prompt completions, using [TextPlugin]. */
suspend fun AiPromptBatch.execute(policy: RunnableExecutionPolicy = RunnableExecutionPolicy()): List<AiPromptTrace> = runConfigs().map {
    val model = TextPlugin.textCompletionModels().firstOrNull { m -> m.modelId == it.second.model }
    if (model == null)
        AiPromptTrace(it.first, it.second, AiPromptExecInfo("Model not found: ${it.second.model}"))
    else
        it.execute(model, policy)
}