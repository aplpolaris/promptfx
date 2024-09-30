package tri.promptfx.tools

import javafx.beans.property.SimpleIntegerProperty
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Model for history of prompts throughout the application. */
class PromptTraceHistoryModel : Component() {
    val prompts = observableListOf<AiPromptTraceSupport<*>>()
    val maxHistorySize = SimpleIntegerProperty(1000)

    init {
        prompts.onChange { checkHistorySize() }
        maxHistorySize.onChange { checkHistorySize() }
    }

    private fun checkHistorySize() {
        val max = maxHistorySize.value
        if (prompts.size > max) {
            prompts.remove(0, prompts.size - max)
        }
    }
}