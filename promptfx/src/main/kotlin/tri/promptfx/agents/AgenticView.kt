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
package tri.promptfx.agents

import com.fasterxml.jackson.databind.JsonNode
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.transformation.FilteredList
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextCompletion
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.impl.ToolChainExecutor
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.tool.JsonMultimodalToolExecutor
import tri.ai.tool.JsonToolExecutor
import tri.ai.tool.ToolExecutableResult
import tri.ai.tool.wf.*
import tri.promptfx.*
import tri.util.info
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [AgenticView]. */
class AgenticPlugin : NavigableWorkspaceViewImpl<AgenticView>("Agents", "Agentic Workflow", WorkspaceViewAffordance.INPUT_ONLY, AgenticView::class)

/** View to execute an agentic workflow where the tools are view with text inputs and outputs. */
class AgenticView : AiPlanTaskView("Agentic Workflow", "Describe a task and any necessary input to use multiple views within a common workflow.") {

    val pfxWorkspace: PromptFxWorkspace by inject()

    private val modelList = PromptFxModels.policy.multimodalModels()
    private val model = SimpleObjectProperty(PromptFxModels.policy.multimodalModelDefault())

    private val input = SimpleStringProperty("")
    private val engine = SimpleObjectProperty(WorkflowEngine.TOOL_CHAIN)
    private val tools = observableListOf<SelectableTool>()
    private val searchText = SimpleStringProperty("")
    private val filteredTools = FilteredList(tools) { true }

    init {
        addInputTextArea(input)

        input {
            // Toolbar for selection and filtering
            toolbar {
                // Menu button for Select All/None
                menubutton("Selection") {
                    item("Select All") {
                        action {
                            tools.forEach { it.selected = true }
                        }
                    }
                    item("Select None") {
                        action {
                            tools.forEach { it.selected = false }
                        }
                    }
                }
                
                spacer()
                
                // Search field
                label("Quick Search:")
                textfield(searchText) {
                    promptText = "Filter by name, category, or description..."
                    prefWidth = 250.0
                }
            }
            
            // Tools list view
            listview(filteredTools) {
                cellFormat {
                    graphic = hbox(5) {
                        checkbox(property = it.selectedProperty)
                        label(it.category) {
                            style {
                                fontWeight = FontWeight.BOLD
                            }
                        }
                        label(it.tool.name)
                        label(" - ${it.tool.description}") {
                            style {
                                textFill = c("#888")
                                fontStyle = FontPosture.ITALIC
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
                    items.setAll(WorkflowEngine.entries)
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
        
        // Setup filtering logic
        searchText.onChange { updateFilter() }
    }
    
    /** Updates the filter based on search text. */
    private fun updateFilter() {
        val query = searchText.value?.lowercase() ?: ""
        filteredTools.setPredicate { tool ->
            if (query.isEmpty()) {
                true
            } else {
                tool.category.lowercase().contains(query) ||
                tool.tool.name.lowercase().contains(query) ||
                tool.tool.description.lowercase().contains(query)
            }
        }
    }

    override fun onDock() {
        if (tools.isEmpty())
            tools.addAll(tools()
                .sortedWith(compareBy({ it.category }, { it.tool.name }))
            )
    }

    /** Get the tools available. */
    private fun tools(): List<SelectableTool> {
        val views: List<PromptFxViewInfo> = pfxWorkspace.viewsWithInputs.values
            .map { it.entries }.flatten().map { it.value }
            .filter { it.view != AgenticView::class.java }
        val tools = views.mapNotNull { info ->
            val view = (info.viewComponent ?: find(info.view!!)) as? AiTaskView
            view?.let { SelectableTool(info.group, it.executable()) }
        }
        return tools
    }

    /** Convert [AiTaskView] to [Executable]. */
    fun AiTaskView.executable() = object : Executable {
        override val name: String = title
        override val description: String = instruction
        override val version: String = "1.0"
        override val inputSchema = null
        override val outputSchema = null
        override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
            // More robust input handling - extract input text from JsonNode
            val inputText = when {
                input.has("input") -> input.get("input").asText()
                input.isTextual -> input.asText()
                else -> input.toString()
            }
            val dict = mapOf("input" to inputText)
            
            val result = runBlocking {
                executeTask(this@executable, dict)
            }
            return MAPPER.createObjectNode().put("result", result.result)
        }
    }

    override fun plan(): AiPlanner = aitask("agent") {
        tools.onEach { it.state = ToolState.NONE }
        val task = input.value
        info<AgenticView>("Executing task using ${engine.value}: $task")
        val selectedTools = tools.filter { it.selected }
        info<AgenticView>("  > Tools available: ${selectedTools.map { it.tool.name }}")

        val tools = selectedTools.map { it.tool }
        val result = when (engine.value) {
            WorkflowEngine.TOOL_CHAIN -> executeToolChain(task, tools).awaitResponse().message.content!!.first().text!!
            WorkflowEngine.JSON_TOOL -> executeJsonTool(task, tools)
            WorkflowEngine.JSON_TOOL_MULTIMODAL -> executeJsonMultimodalTool(task, tools)
            WorkflowEngine.WORKFLOW_PLANNER -> executeWorkflowPlanner(task, tools)
        }

        runLater {
            pfxWorkspace.dock(this)
        }
        AiPromptTrace(outputInfo = AiOutputInfo.text(result))
    }.planner

    //region WORKFLOW EXECUTION METHODS

    /** Executes using [tri.ai.tool.ToolChainExecutor]. */
    private fun executeToolChain(task: String, selectedTools: List<Executable>) =
        ToolChainExecutor(selectedTools)
            .sendMessage(AgentChatSession(config = AgentChatConfig(modelId = controller.chatService.value.modelId)),
                MultimodalChatMessage.user(task))

    /** Executes using [tri.ai.tool.JsonToolExecutor]. */
    private fun executeJsonTool(task: String, selectedTools: List<Executable>): String {
        val exec = JsonToolExecutor(model.value!!, selectedTools)
        return runBlocking {
            exec.execute(task)
        }
    }

    /** Executes using [tri.ai.tool.JsonMultimodalToolExecutor]. */
    private fun executeJsonMultimodalTool(task: String, selectedTools: List<Executable>) = runBlocking {
        JsonMultimodalToolExecutor(model.value!!, selectedTools)
            .execute(task)
    }

    /** Executes using [tri.ai.tool.wf.WorkflowExecutor]. */
    private fun executeWorkflowPlanner(task: String, selectedTools: List<Executable>): String {
        val textCompletion = controller.completionEngine.value
        val exec = WorkflowExecutor(
            WExecutorChat(textCompletion, maxTokens = 2000, temp = 0.5),
            selectedTools.map { it.toSolver(textCompletion) }
        )
        val request = WorkflowUserRequest(task)
        return exec.solve(request).finalResult().toString()
    }

    //endregion

    /** Executes a view's tool with specified input. Switches to show the view while executing. */
    private fun executeTask(view: AiTaskView, input: Map<String, String>): ToolExecutableResult {
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
                view.inputArea()?.text = input["input"]
                val task = runBlocking {
                    view.processUserInput()
                }
                view.taskCompleted(task)
                val finalResult = task.finalResult.values?.firstOrNull()?.textContent()
                result.complete(finalResult ?: "(no final result)")
            }
        }
        val res = runBlocking { result.await() }
        return ToolExecutableResult(res)
    }

    companion object {
        /** Converts a [SelectableTool] to a [tri.ai.tool.wf.WorkflowSolver]. */
        private fun Executable.toSolver(textCompletion: TextCompletion) = object : WorkflowSolver(name, description, mapOf("input" to "Input for $name"), mapOf("result" to "Result from $name")) {
            override suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep {
                val t0 = System.currentTimeMillis()
                val input = state.aggregateInputsFor(name).values.mapNotNull { it?.value }.ifEmpty {
                    listOf(task.name)
                }.joinToString("\n")
                val result = runBlocking {
                    this@toSolver.execute(
                        MAPPER.createObjectNode().put("input", input),
                        ExecContext(resources = mapOf("textCompletion" to textCompletion))
                    )
                }
                val tt = System.currentTimeMillis() - t0
                return solveStep(task, inputs(input), outputs(result), tt, true)
            }
        }

        /** UI representation of a tool that can be selected. */
        private class SelectableTool(val category: String, val tool: Executable) {
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
