package tri.promptfx.ui

import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace

/** UI for a list of [AiPromptTrace]s. */
class PromptTraceCardList: Fragment() {

    val prompts = observableListOf<AiPromptTrace>()

    override val root = listview(prompts) {
        cellFormat {
            graphic = PromptTraceCard().apply { setTrace(it) }.root
        }
    }
    
}