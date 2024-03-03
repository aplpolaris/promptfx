package tri.ai.prompt.run

import tri.ai.core.TextCompletion
import tri.ai.prompt.AiPrompt.Companion.fill
import tri.ai.prompt.trace.*

/**
 * Executes a text completion with a single configuration.
 * Overwrites the model id in the configuration to match the model.
 * @param completion the text completion model
 * @param policy the policy for re-attempting failed completions
 * @return trace of the execution, including output and run info
 */
suspend fun AiPromptRunConfig.execute(completion: TextCompletion, policy: AiPromptExecutionPolicy = AiPromptExecutionPolicy()): AiPromptTrace {
    second.model = completion.modelId
    val promptText = first.filled()
    return try {
        // TODO - implement retries and delayed query policy
        val t0 = System.currentTimeMillis()
        val output = completion.complete(promptText, second)
        val completionTime = System.currentTimeMillis() - t0
        val runInfo = if (output.error != null) {
            AiPromptExecInfo.error(output.error.message)
        } else {
            AiPromptExecInfo()
        }.apply {
            responseTimeMillis = completionTime
        }
        AiPromptTrace(first, second, runInfo, AiPromptOutputInfo(output.value))
    } catch (x: Exception) {
        AiPromptTrace(first, second, AiPromptExecInfo.error(x.message))
    }
}

suspend fun TextCompletion.complete(prompt: String, modelInfo: AiPromptModelInfo) =
    complete(prompt,
        modelInfo.modelParams["maxTokens"] as? Int,
        modelInfo.modelParams["temperature"] as? Double,
        modelInfo.modelParams["stop"] as? String
    )

fun AiPromptInfo.filled() = prompt.fill(promptParams)