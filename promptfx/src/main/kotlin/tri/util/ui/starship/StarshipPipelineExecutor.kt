package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.TextChat
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.core.tool.impl.PromptChatRegistry
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskMonitor
import tri.ai.pips.PrintMonitor
import tri.ai.pips.api.PPlan
import tri.ai.pips.api.PPlanExecutor
import tri.ai.prompt.PromptLibrary
import tri.promptfx.PromptFxWorkspace

/** Executes a Starship pipeline with observability tracking. */
class StarshipPipelineExecutor(
    /** The question generator. */
    val questionConfig: StarshipConfigQuestion,
    /** The plan to execute. */
    val plan: PPlan,
    /** Chat model used for prompt execution. */
    val chat: TextChat,
    /** Optional delay between steps. */
    val stepDelay: Int = 0,
    /** Used for sending inputs to UI components for processing. */
    val workspace: PromptFxWorkspace,
    /** Used for sending inputs to the view that was active when Starship was launched. */
    val baseComponentTitle: String,
    /** Used for collecting results. */
    val results: StarshipPipelineResults
) {
    suspend fun execute() {
        val registry = ExecutableRegistry.Companion.create(
            listOf(StarshipExecutableQuestionGenerator(questionConfig, chat), StarshipExecutableCurrentView(workspace, baseComponentTitle)) +
                    PromptChatRegistry(PromptLibrary.Companion.INSTANCE, chat).list()
        )
        val context = ExecContext().apply {
            variableSet = results::updateVariable
        }
        val monitor = if (stepDelay == 0) PrintMonitor() else PrintMonitorWithDelay(stepDelay)
        PPlanExecutor(registry).execute(plan, context, monitor)
    }

    private inner class PrintMonitorWithDelay(val delayMs: Int) : AiTaskMonitor {
        val printer = PrintMonitor()
        override fun taskStarted(task: AiTask) = printer.taskStarted(task)
        override fun taskUpdate(task: AiTask, progress: Double) = printer.taskUpdate(task, progress)
        override fun taskCompleted(task: AiTask, result: Any?) {
            printer.taskCompleted(task, result)
            Thread.sleep(delayMs.toLong())
        }
        override fun taskFailed(task: AiTask, error: Throwable) {
            printer.taskFailed(task, error)
            Thread.sleep(delayMs.toLong())
        }
    }
}

/** Unwraps a [JsonNode] to get a text value, working iteratively until finding a string value or a more complex structure. */
fun JsonNode.unwrappedTextValue(): String = when {
    isTextual -> asText()
    isObject && size() == 1 -> properties().first().value.unwrappedTextValue()
    else -> toString()
}