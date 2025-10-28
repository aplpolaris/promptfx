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
import tri.ai.core.TextChat
import tri.ai.core.agent.*
import tri.ai.core.agent.impl.JsonToolExecutor
import tri.ai.core.agent.impl.ToolChainExecutor
import tri.ai.core.agent.wf.*
import tri.ai.core.textContent
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.JsonToolExecutable.Companion.OUTPUT_SCHEMA
import tri.ai.core.tool.JsonToolExecutable.Companion.STRING_INPUT_SCHEMA
import tri.ai.core.tool.ToolExecutableResult
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.*
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [AgenticView]. */
class AgenticPlugin : NavigableWorkspaceViewImpl<AgenticView>("Agents", "Agentic Workflow", WorkspaceViewAffordance.INPUT_ONLY, AgenticView::class)

/** View to execute an agentic workflow where the tools are view with text inputs and outputs. */
class AgenticView : AiPlanTaskView("Agentic Workflow", "Describe a task and any necessary input to use multiple views within a common workflow.") {

    val pfxWorkspace: PromptFxWorkspace by inject()

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
                combobox(controller.chatService, PromptFxModels.policy.chatModels())
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
        override val name: String = title.asToolName()
        override val description: String = instruction
        override val version: String = "1.0"
        override val inputSchema = MAPPER.readTree(STRING_INPUT_SCHEMA)
        override val outputSchema = MAPPER.readTree(OUTPUT_SCHEMA)
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
            return createObject("result", result.result)
        }
    }

    /** Converts a general name to a tool-compatible name. */
    // replace all spaces and punctuation that is not a dash with underscore, then replace multiple underscores with single, and lowercase
    private fun String.asToolName() = replace(" ", "_")
        .replace(Regex("_+"), "_")
        .lowercase()

    override fun plan(): AiPlanner = aitask("agent") {
        tools.onEach { it.state = ToolState.NONE }

        // pick the right executor
        val tools = tools.filter { it.selected }.map { it.tool }
        val config = AgentChatConfig(modelId = controller.chatService.value!!.modelId)
        val executor: AgentChat =  when (engine.value) {
            WorkflowEngine.TOOL_CHAIN -> ToolChainExecutor(tools)
            WorkflowEngine.JSON_TOOL -> JsonToolExecutor(tools)
            WorkflowEngine.WORKFLOW_PLANNER -> WorkflowExecutor(WorkflowExecutorChat(config), tools.map { it.toSolver(controller.chatService.value) })
        }

        // set up task execution flow
        val task = input.value
        val flow = executor.sendMessage(AgentChatSession(config = config), MultimodalChatMessage.user(task))
        val result = flow.awaitResponseWithLogging().message.textContent()!!

        runLater { pfxWorkspace.dock(this) }
        AiPromptTrace(outputInfo = AiOutputInfo.text(result))
    }.planner

    /** Executes a view's tool with specified input. Switches to show the view while executing. */
    private fun executeTask(view: AiTaskView, input: Map<String, String>): ToolExecutableResult {
        val result = CompletableDeferred<String>()
        runLater {
            pfxWorkspace.dock(view)
            tools.onEach {
                if (it.state == ToolState.ACTIVE)
                    it.state = ToolState.USED
            }
            tools.firstOrNull { it.tool.name == view.title.asToolName() }?.let {
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
        /** Converts a [SelectableTool] to a [tri.ai.core.agent.wf.WorkflowSolver]. */
        private fun Executable.toSolver(textChat: TextChat) = object : WorkflowSolver(name, description, mapOf("input" to "Input for $name"), mapOf("result" to "Result from $name")) {
            override suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep {
                val t0 = System.currentTimeMillis()
                val input = state.aggregateInputsFor(name).values.mapNotNull { 
                    it?.let { node -> 
                        if (node.isTextual) node.asText() else node.toString()
                    }
                }.ifEmpty {
                    listOf(task.name)
                }.joinToString("\n")
                val result = runBlocking {
                    this@toSolver.execute(
                        createObject("input", input),
                        ExecContext(resources = mapOf("textChat" to textChat))
                    )
                }.get("result").asText()
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
            WORKFLOW_PLANNER("Workflow Planner");

            override fun toString() = text
        }
    }

}
