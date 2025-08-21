package tri.promptfx

import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.fill

/** Unified access to global objects within [PromptFx]. */
object PromptFxGlobals {

    /** Prompt library. */
    val promptLibrary = PromptLibrary.INSTANCE
    /** Additional prompts from runtime views. */
    val runtimeViewPromptLibrary = RuntimePromptViewConfigs.PROMPT_LIBRARY

    /** Gets prompt ids with a given prefix. */
    fun promptsWithPrefix(prefix: String) =
        promptLibrary.list(prefix = prefix).map { it.id }

    /** Lookup a prompt with given id. */
    fun lookupPrompt(promptId: String) =
        promptLibrary.get(promptId) ?: runtimeViewPromptLibrary.get(promptId) ?: error("Prompt '$promptId' not found in library")

    /** Lookup a prompt with given id, or null if not found. */
    fun lookupPromptOrNull(promptId: String): PromptDef? =
        promptLibrary.get(promptId) ?: runtimeViewPromptLibrary.get(promptId)

    /** Fills a prompt with the given values. */
    fun fillPrompt(promptId: String, vararg values: Pair<String, Any>) =
        lookupPrompt(promptId).fill(*values)

}
