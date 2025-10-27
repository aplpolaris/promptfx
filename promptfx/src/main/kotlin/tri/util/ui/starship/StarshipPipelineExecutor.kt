package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import tri.ai.core.TextChat
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.core.tool.impl.PromptChatRegistry
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskMonitor
import tri.ai.pips.PrintMonitor
import tri.ai.pips.api.AiPlanStepTask
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
    /** Optional delay applied after each step. */
    val stepDelayMs: Int = 0,
    /** Used for sending inputs to UI components for processing. */
    val workspace: PromptFxWorkspace,
    /** Used for sending inputs to the view that was active when Starship was launched. */
    val baseComponentTitle: String,
    /** Used for collecting results. */
    val results: StarshipPipelineResults
) {

    /** Custom monitor for managing delay and tracking completed steps. */
    val monitor = object : AiTaskMonitor {
        val print = PrintMonitor()
        override fun taskStarted(task: AiTask) {
            print.taskStarted(task)
        }
        override fun taskUpdate(task: AiTask, progress: Double) {
            print.taskUpdate(task, progress)
        }
        override fun taskCompleted(task: AiTask, result: Any?) {
            print.taskCompleted(task, result)
            if (task is AiPlanStepTask) {
                results.activeStepVar.set(task.step.saveAs)
            }
            Thread.sleep(stepDelayMs.toLong())
        }
        override fun taskFailed(task: AiTask, error: Throwable) {
            print.taskFailed(task, error)
            Thread.sleep(stepDelayMs.toLong())
        }
    }

    suspend fun execute() {
        val registry = ExecutableRegistry.Companion.create(
            listOf(StarshipExecutableQuestionGenerator(questionConfig, chat), StarshipExecutableCurrentView(workspace, baseComponentTitle)) +
                    PromptChatRegistry(PromptLibrary.Companion.INSTANCE, chat).list()
        )
        val context = ExecContext().apply {
            variableSet = results::updateVariable
            results.getMultiChoiceValues().forEach {
                put(it.key, TextNode.valueOf(it.value))
            }
        }
        PPlanExecutor(registry).execute(plan, context, monitor)
    }
}

/** Unwraps a [JsonNode] to get a text value, working iteratively until finding a string value or a more complex structure. */
fun JsonNode.unwrappedTextValue(): String = when {
    isTextual -> asText()
    isObject && size() == 1 -> properties().first().value.unwrappedTextValue()
    else -> toString()
}