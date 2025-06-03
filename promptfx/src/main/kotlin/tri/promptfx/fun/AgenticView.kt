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

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import tornadofx.*
import tri.ai.openai.OpenAiAdapter
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.tool.JsonMultimodalToolExecutor
import tri.ai.tool.JsonTool
import tri.ai.tool.JsonToolExecutor
import tri.ai.tool.Tool
import tri.ai.tool.ToolChainExecutor
import tri.ai.tool.ToolDict
import tri.ai.tool.ToolResult
import tri.ai.tool.input
import tri.ai.tool.wf.WExecutorChat
import tri.ai.tool.wf.WorkflowExecutor
import tri.ai.tool.wf.WorkflowSolveStep
import tri.ai.tool.wf.WorkflowSolver
import tri.ai.tool.wf.WorkflowState
import tri.ai.tool.wf.WorkflowTask
import tri.ai.tool.wf.WorkflowUserRequest
import tri.promptfx.AiPlanTaskView
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxViewInfo
import tri.promptfx.PromptFxWorkspace
import tri.util.info
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [AgenticView]. */
class AgenticPlugin : NavigableWorkspaceViewImpl<AgenticView>("Fun", "Agentic Workflow", WorkspaceViewAffordance.INPUT_ONLY, AgenticView::class)

/** View to execute an agentic workflow where the tools are view with text inputs and outputs. */
class AgenticView : AiPlanTaskView("Agentic Workflow", "Describe a task and any necessary input to use multiple views within a common workflow.") {

    val pfxWorkspace: PromptFxWorkspace by inject()

    private val modelList = PromptFxModels.policy.multimodalModels()
    private val model = SimpleObjectProperty(PromptFxModels.policy.multimodalModelDefault())

    private val input = SimpleStringProperty("")
    private val engine = SimpleObjectProperty(WorkflowEngine.TOOL_CHAIN)
    private val tools = observableListOf<SelectableTool>()

    init {
        addInputTextArea(input)

        input {
            listview(tools) {
                cellFormat {
                    graphic = hbox(5) {
                        checkbox(property = it.selectedProperty)
                        label(it.tool.name)
                        label(" - ${it.tool.description}") {
                            style {
                                textFill = c("#888")
                                fontStyle = javafx.scene.text.FontPosture.ITALIC
                            }
                        }
                        // background light yellow if used, light green if active, adjusted when properties change
                        it.stateProperty.onChange { state ->
                            when (state) {
                                ToolState.NONE -> style {
                                    backgroundColor += c("#f0f0f0")
                                }
                                ToolState.USED -> style {
                                    backgroundColor += c("#ffffe0")
                                }
                                ToolState.ACTIVE -> style {
                                    backgroundColor += c("#f0fff0")
                                }
                                null -> TODO()
                            }
                        }
                    }
                }
            }
        }
        parameters("Workflow Options") {
            field("Engine") {
                combobox<WorkflowEngine>(engine) {
                    items.setAll(WorkflowEngine.values().toList())
                    value = WorkflowEngine.TOOL_CHAIN
                }
            }
            field("Model") {
                enableWhen {
                    engine.isEqualTo(WorkflowEngine.JSON_TOOL_MULTIMODAL)
                }
                combobox(model, modelList)
            }
        }
    }

    override fun onDock() {
        if (tools.isEmpty())
            tools.addAll(tools().map { SelectableTool(it) })
    }

    /** Get the tools available. */
    private fun tools(): List<Tool> {
        val views: List<PromptFxViewInfo> = pfxWorkspace.viewsWithInputs.values
            .map { it.entries }.flatten().map { it.value }
            .filter { it.view != AgenticView::class.java }
        val tools = views.map { info ->
            val view = (info.viewComponent ?: find(info.view!!)) as AiTaskView
            object : Tool(info.name, view.instruction, requiresLlm = true) {
                override suspend fun run(input: ToolDict) = executeTask(view, input)
            }
        }
        return tools
    }

    override fun plan(): AiPlanner = aitask("agent") {
        tools.onEach { it.state = ToolState.NONE }
        val task = input.value
        info<AgenticView>("Executing task using ${engine.value}: $task")
        val selectedTools = tools.filter { it.selected }.map { it.tool }
        info<AgenticView>("  > Tools available: ${selectedTools.map { it.name }}")

        val result = when (engine.value) {
            WorkflowEngine.TOOL_CHAIN -> executeToolChain(task, selectedTools)
            WorkflowEngine.JSON_TOOL -> executeJsonTool(task, selectedTools)
            WorkflowEngine.JSON_TOOL_MULTIMODAL -> executeJsonMultimodalTool(task, selectedTools)
            WorkflowEngine.WORKFLOW_PLANNER -> executeWorkflowPlanner(task, selectedTools)
        }

        runLater {
            pfxWorkspace.dock(this)
        }
        AiPromptTrace(outputInfo = AiOutputInfo.output(result))
    }.planner

    /** Executes using [ToolChainExecutor]. */
    private fun executeToolChain(task: String, selectedTools: List<Tool>): String {
        val exec = ToolChainExecutor(controller.completionEngine.value)
        return exec.executeChain(task, selectedTools)
    }

    /** Executes using [JsonToolExecutor]. */
    private fun executeJsonTool(task: String, selectedTools: List<Tool>): String {
        // TODO - remove dependence on specific OpenAI adapter
        val exec = JsonToolExecutor(OpenAiAdapter.INSTANCE, controller.chatService.value.modelId, selectedTools.map { it.toJsonTool() })
        return runBlocking {
            exec.execute(task)
        }
    }

    /** Executes using [JsonMultimodalToolExecutor]. */
    private fun executeJsonMultimodalTool(task: String, selectedTools: List<Tool>): String {
        val exec = JsonMultimodalToolExecutor(model.value!!, selectedTools.map { it.toJsonTool() })
        return runBlocking {
            exec.execute(task)
        }
    }

    /** Executes using [WorkflowExecutor]. */
    private fun executeWorkflowPlanner(task: String, selectedTools: List<Tool>): String {
        val exec = WorkflowExecutor(
            WExecutorChat(controller.completionEngine.value, maxTokens = 2000, temp = 0.5),
            selectedTools.map { it.toSolver() }
        )
        val request = WorkflowUserRequest(task)
        return exec.solve(request).finalResult().toString()
    }


    /** Executes a view's tool with specified input. Switches to show the view while executing. */
    private fun executeTask(view: AiTaskView, input: ToolDict): ToolResult {
        val result = CompletableDeferred<String>()
        runLater {
            pfxWorkspace.dock(view)
            tools.onEach {
                if (it.state == ToolState.ACTIVE)
                    it.state = ToolState.USED
            }
            tools.firstOrNull { it.tool.name == view.title }?.let {
                it.state = ToolState.ACTIVE
            }
        }
        runBlocking {
            runLater {
                view.inputArea()?.text = input.input
                val task = runBlocking {
                    view.processUserInput()
                }
                view.taskCompleted(task)
                task.finalResult.let {
                    result.complete(it.firstValue.toString())
                }
            }
        }
        val res = runBlocking { result.await() }
        return ToolResult(res)
    }

    companion object {
        private val JSON_SCHEMA_STRING_INPUT = """{"type":"object","properties":{"input":{"type":"string"}}}"""

        /** Converts a [Tool] to a [JsonTool]. */
        private fun Tool.toJsonTool() = object : JsonTool(name, description, JSON_SCHEMA_STRING_INPUT) {
            override suspend fun run(input: JsonObject): String {
                val dict = input.toToolDict()
                return this@toJsonTool.run(dict).finalResult!!
            }
        }

        /** Converts a [JsonObject] to a [ToolDict]. */
        private fun JsonObject.toToolDict(): ToolDict {
            val input = this["input"]?.jsonPrimitive?.content ?: ""
            return mapOf("input" to input)
        }

        /** Converts a [Tool] to a [WorkflowSolver]. */
        private fun Tool.toSolver() = object : WorkflowSolver(name, description, mapOf("input" to "Input for $name"), mapOf("result" to "Result from $name")) {
            override suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep {
                val t0 = System.currentTimeMillis()
                val input = state.aggregateInputsFor(name).values.firstOrNull()?.value ?: ""
                val dict = mapOf("input" to input) as ToolDict
                val result = runBlocking { this@toSolver.run(dict) }
                val tt = System.currentTimeMillis() - t0
                return solveStep(task, inputs(input), outputs(result), tt, true)
            }
        }

        /** UI representation of a tool that can be selected. */
        private class SelectableTool(val tool: Tool) {
            val selectedProperty = SimpleBooleanProperty(true)
            var selected by selectedProperty
            val stateProperty = SimpleObjectProperty(ToolState.NONE)
            var state by stateProperty
        }

        /** Tracks execution state of a given tool. */
        private enum class ToolState { NONE, USED, ACTIVE }

        /** Selection options for workflow execution engines. */
        private enum class WorkflowEngine(val text: String) {
            TOOL_CHAIN("Tool Chain"),
            JSON_TOOL("Tool Chain with JSON Schemas"),
            JSON_TOOL_MULTIMODAL("Tool Chain with JSON Schemas (Multimodal model)"),
            WORKFLOW_PLANNER("Workflow Planner");

            override fun toString() = text
        }
    }

}

