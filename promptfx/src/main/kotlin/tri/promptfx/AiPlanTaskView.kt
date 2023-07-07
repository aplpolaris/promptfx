package tri.promptfx

import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPlanner

/** View that gets result from a planned set of tasks. */
abstract class AiPlanTaskView(title: String, description: String) : AiTaskView(title, description) {

    override suspend fun processUserInput() =
        AiPipelineExecutor.execute(plan().plan(), progress)

    abstract fun plan(): AiPlanner

}

