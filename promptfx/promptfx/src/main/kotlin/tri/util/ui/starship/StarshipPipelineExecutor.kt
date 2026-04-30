/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import tri.ai.core.TextChat
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.core.tool.impl.PromptChatRegistry
import kotlinx.coroutines.flow.FlowCollector
import tri.ai.pips.ExecEvent
import tri.ai.pips.PrintMonitor
import tri.ai.pips.api.AiPlanStepTask
import tri.ai.pips.api.PPlan
import tri.ai.pips.api.PPlanExecutor
import tri.ai.pips.api.PPlanStep
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
    val monitor = object : FlowCollector<ExecEvent> {
        val print = PrintMonitor()
        /** Pre-built identity-keyed index map for O(1) step lookups. */
        val stepIndex: Map<PPlanStep, Int> = java.util.IdentityHashMap<PPlanStep, Int>().also { map ->
            plan.steps.forEachIndexed { i, s -> map[s] = i }
        }
        override suspend fun emit(value: ExecEvent) {
            print.emit(value)
            when (value) {
                is ExecEvent.TaskStarted -> {
                    val task = value.task
                    if (task is AiPlanStepTask) {
                        stepIndex[task.step]?.let { results.setStepState(it, PipelineStepState.ACTIVE) }
                    }
                }
                is ExecEvent.TaskCompleted -> {
                    val completedTask = value.task
                    if (completedTask is AiPlanStepTask) {
                        results.activeStepVar.set(completedTask.step.saveAs)
                        stepIndex[completedTask.step]?.let { results.setStepState(it, PipelineStepState.DONE) }
                    }
                    Thread.sleep(stepDelayMs.toLong())
                }
                is ExecEvent.TaskFailed -> {
                    val failedTask = value.task
                    if (failedTask is AiPlanStepTask) {
                        stepIndex[failedTask.step]?.let { results.setStepState(it, PipelineStepState.FAILED) }
                    }
                    Thread.sleep(stepDelayMs.toLong())
                }
                else -> {}
            }
        }
    }

    suspend fun execute() {
        results.initSteps(plan.steps)
        results.started.set(true)
        val registry = ExecutableRegistry.create(
            listOf(StarshipExecutableQuestionGenerator(questionConfig, chat), StarshipExecutableCurrentView(workspace, baseComponentTitle)) +
                    PromptChatRegistry(PromptLibrary.INSTANCE, chat).list()
        )
        val context = ExecContext(monitor = monitor).apply {
            variableSet = results::updateVariable
            results.getMultiChoiceValues().forEach {
                put(it.key, TextNode.valueOf(it.value))
            }
        }
        PPlanExecutor(registry).execute(plan, context)
        results.completed.set(true)
    }
}

/** Unwraps a [JsonNode] to get a text value, working iteratively until finding a string value or a more complex structure. */
fun JsonNode.unwrappedTextValue(): String = when {
    isTextual -> asText()
    isObject && size() == 1 -> properties().first().value.unwrappedTextValue()
    else -> toString()
}
