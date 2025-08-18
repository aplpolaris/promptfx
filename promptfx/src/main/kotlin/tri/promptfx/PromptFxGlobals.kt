package tri.promptfx

import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.fill

/** Unified access to global objects within [PromptFx]. */
object PromptFxGlobals {

    /** Prompt library. */
    val promptLibrary = PromptLibrary.INSTANCE

    /** Gets prompt ids with a given prefix. */
    fun promptsWithPrefix(prefix: String) =
        promptLibrary.list(prefix = prefix).map { it.id }

    /** Lookup a prompt with given id. */
    fun lookupPrompt(promptId: String) =
        promptLibrary.get(promptId) ?: error("Prompt '$promptId' not found in library")

    /** Fills a prompt with the given values. */
    fun fillPrompt(promptId: String, vararg values: Pair<String, Any>) =
        promptLibrary.get(promptId)?.fill(*values) ?: error("Prompt '$promptId' not found in library")

}
