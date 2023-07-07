package tri.promptfx.api

import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView

class FineTuneView : AiTaskView("Fine-tuning", "This page is not yet implemented.") {

    override suspend fun processUserInput() = AiPipelineResult.todo()

}