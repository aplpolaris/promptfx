package tri.promptfx

/**
 * Global manager for models available within PromptFx.
 * Model availability is determined by the current [PromptFxPolicy].
 */
object PromptFxModels {
    val policy: PromptFxPolicy = PromptFxPolicyUnrestricted
//    val policy: PromptFxPolicy = PromptFxPolicyOpenAi

    fun textCompletionModels() = policy.textCompletionModels()
    fun textCompletionModelDefault() = policy.textCompletionModelDefault()
    fun embeddingModels() = policy.embeddingModels()
    fun embeddingModelDefault() = policy.embeddingModelDefault()
    fun chatModels() = policy.chatModels()
    fun chatModelDefault() = policy.chatModelDefault()
//    fun imageModels() = policy.imageModels()
//    fun imageModelDefault() = policy.imageModelDefault()
}