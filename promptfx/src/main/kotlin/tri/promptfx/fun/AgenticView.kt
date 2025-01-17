/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.`fun`

import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import tornadofx.*
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO_ID
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.tool.JsonTool
import tri.ai.tool.JsonToolExecutor
import tri.ai.tool.Tool
import tri.ai.tool.ToolChainExecutor
import tri.promptfx.*
import tri.util.info
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [AgenticView]. */
class AgenticPlugin : NavigableWorkspaceViewImpl<AgenticView>("Fun", "Agentic Workflow", WorkspaceViewAffordance.INPUT_ONLY, AgenticView::class)

/** View to execute an agentic workflow where the tools are view with text inputs and outputs. */
class AgenticView : AiPlanTaskView("Agentic Workflow", "Describe a task and any necessary input to use multiple views within a common workflow.") {

    val pfxWorkspace: PromptFxWorkspace by inject()
    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
    }

    override fun plan(): AiPlanner = aitask("agent") {
        executeToolChain()
    }.planner

    /** Executes using [ToolChainExecutor]. */
    private fun executeToolChain(): AiPromptTrace<String> {
        val task = input.value
        info<AgenticView>("Executing task using tools: $task")
        val views: List<PromptFxViewInfo> = pfxWorkspace.viewsWithInputs.values
            .map { it.entries }.flatten().map { it.value }
        val tools = views.map { info ->
            val view = (info.viewComponent ?: find(info.view!!)) as AiTaskView
            object : Tool(info.name, view.instruction, null, requiresLlm = true, isTerminal = false) {
                override suspend fun run(input: String) = executeTask(view, input)
            }
        }
        val result = ToolChainExecutor(controller.completionEngine.value)
            .executeChain(task, tools)
        return AiPromptTrace(outputInfo = AiOutputInfo.output(result))
    }

    private val JSON_SCHEMA_STRING_INPUT = """{"type":"object","properties":{"input":{"type":"string"}}}"""

    /** Executes using [JsonToolExecutor]. */
    private suspend fun executeJsonTools(): AiPromptTrace<String> {
        val task = input.value
        info<AgenticView>("Executing task using tools: $task")
        val views: List<PromptFxViewInfo> = pfxWorkspace.viewsWithInputs.values
            .map { it.entries }.flatten().map { it.value }
        val tools = views.map { info ->
            val view = (info.viewComponent ?: find(info.view!!)) as AiTaskView
            object : JsonTool(info.name, view.instruction, JSON_SCHEMA_STRING_INPUT) {
                override suspend fun run(input: JsonObject) =
                    executeTask(view, input["input"]?.jsonPrimitive?.content ?: "No input")
            }
        }
        val result = JsonToolExecutor(OpenAiClient.INSTANCE, GPT35_TURBO_ID, tools)
            .execute(task)
        return AiPromptTrace(
            modelInfo = AiModelInfo.info(GPT35_TURBO_ID),
            outputInfo = AiOutputInfo.output(result)
        )
    }

    fun executeTask(view: AiTaskView, input: String): String {
        val result = CompletableDeferred<String>()
        runBlocking {
            runLater {
                view.inputArea()?.text = input
                val task = runBlocking {
                    view.processUserInput()
                }
                task.finalResult.let {
                    result.complete(it.firstValue.toString())
                }
            }
        }
        return runBlocking { result.await() }
    }

}
