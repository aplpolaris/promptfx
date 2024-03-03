package tri.ai.prompt.run

import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo

typealias AiPromptRunConfig = Pair<AiPromptInfo, AiPromptModelInfo>

/** Provides a series of prompt/model pairings for execution. */
interface AiPromptBatch {

    /** Get all run configs within this series. */
    fun runConfigs(): Iterable<AiPromptRunConfig>

}