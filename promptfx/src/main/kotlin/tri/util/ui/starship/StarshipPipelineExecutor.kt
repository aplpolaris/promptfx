package tri.util.ui.starship

import javafx.scene.image.Image
import tri.ai.core.TextChat
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.core.tool.impl.PromptChatRegistry
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskMonitor
import tri.ai.pips.api.PPlan
import tri.ai.pips.api.PPlanExecutor
import tri.ai.pips.api.PPlanStep
import tri.ai.prompt.PromptLibrary
import tri.ai.text.docs.FormattedText
import tri.util.ui.DocumentThumbnail

/** Executes a Starship pipeline with observability tracking. */
class StarshipPipelineExecutor(val plan: PPlan, val chat: TextChat, val results: StarshipPipelineResults) {
    suspend fun execute() {
        val registry = ExecutableRegistry.Companion.create(
            listOf(StarshipExecutableQuestionGenerator(chat), StarshipExecutableCurrentView()) +
                    PromptChatRegistry(PromptLibrary.Companion.INSTANCE, chat).list()
        )
        // TODO - add observability and update interim results
        val context = ExecContext()
        val monitor = object : AiTaskMonitor {
            override fun taskStarted(task: AiTask) { }
            override fun taskUpdate(task: AiTask, progress: Double) { }
            override fun taskCompleted(task: AiTask, result: Any?) {
                // TODO - potential hook to trigger pipeline result updates
                println("Task completed: ${task.id} with result: $result")
            }
            override fun taskFailed(task: AiTask, error: Throwable) { }
        }
        PPlanExecutor(registry).execute(plan, context, monitor)
        context.variableSet = { name, value ->
            // TODO - potential hook to capture individual variable updates
            println("Variable updated: $name = $value")
        }
    }
}

/** Result object for individual [PPlanStep] steps in the [StarshipView] pipeline. */
class StarshipPipelineStepResult(val step: Int, val label: String, val text: FormattedText, val image: Image?, val docs: List<DocumentThumbnail>) {
    val rawText
        get() = text.toString()
}