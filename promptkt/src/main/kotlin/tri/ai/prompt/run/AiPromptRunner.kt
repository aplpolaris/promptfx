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
suspend fun AiPromptRunConfig.execute(completion: TextCompletion, policy: RunnableExecutionPolicy = RunnableExecutionPolicy()): AiPromptTrace {
    second.model = completion.modelId
    val promptText = first.filled()
    val result = policy.execute { completion.complete(promptText, second) }
    return AiPromptTrace(first, second, AiPromptExecInfo(result.exception?.message), AiPromptOutputInfo(result.value?.value)).apply {
        execInfo.responseTimeMillis = result.duration.toMillis()
    }
}

suspend fun TextCompletion.complete(prompt: String, modelInfo: AiPromptModelInfo) =
    complete(prompt,
        modelInfo.modelParams["maxTokens"] as? Int,
        modelInfo.modelParams["temperature"] as? Double,
        modelInfo.modelParams["stop"] as? String
    )

fun AiPromptInfo.filled() = prompt.fill(promptParams)