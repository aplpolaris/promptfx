package tri.ai.core

import tri.ai.pips.AiTaskResult

/** Interface for text completion. */
interface TextCompletion {

    val modelId: String

    /** Completes user text. */
    suspend fun complete(
        text: String,
        tokens: Int? = 150,
        stop: String? = null
    ): AiTaskResult<String>

}