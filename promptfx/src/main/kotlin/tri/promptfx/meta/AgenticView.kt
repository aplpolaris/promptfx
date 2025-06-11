package tri.promptfx.meta

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
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
import tri.ai.tool.wf.*
import tri.promptfx.AiPlanTaskView
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxViewInfo
import tri.promptfx.PromptFxWorkspace
import tri.util.info
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [AgenticView]. */
class AgenticPlugin : NavigableWorkspaceViewImpl<AgenticView>("Meta", "Agentic Workflow", WorkspaceViewAffordance.Companion.INPUT_ONLY, AgenticView::class)

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
                        label(it.category) {
                            style {
                                fontWeight = FontWeight.BOLD
                            }
                        }
                        label(it.name)
                        label(" - ${it.description}") {
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
            tools.addAll(tools()
                .sortedWith(compareBy({ it.category }, { it.name }))
            )
    }

    /** Get the tools available. */
    private fun tools(): List<SelectableTool> {
        val views: List<PromptFxViewInfo> = pfxWorkspace.viewsWithInputs.values
            .map { it.entries }.flatten().map { it.value }
            .filter { it.view != AgenticView::class.java }
        val tools = views.map { info ->
            val view = (info.viewComponent ?: find(info.view!!)) as AiTaskView
            object : SelectableTool(info.name, view.instruction, info.group) {
                override suspend fun run(input: ToolDict) = executeTask(view, input)
            }
        }
        return tools
    }

    override fun plan(): AiPlanner = aitask("agent") {
        tools.onEach { it.state = ToolState.NONE }
        val task = input.value
        info<AgenticView>("Executing task using ${engine.value}: $task")
        val selectedTools = tools.filter { it.selected }
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
        AiPromptTrace(outputInfo = AiOutputInfo.Companion.output(result))
    }.planner

    //region WORKFLOW EXECUTION METHODS

    /** Executes using [tri.ai.tool.ToolChainExecutor]. */
    private fun executeToolChain(task: String, selectedTools: List<Tool>): String {
        val exec = ToolChainExecutor(controller.completionEngine.value)
        return exec.executeChain(task, selectedTools)
    }

    /** Executes using [tri.ai.tool.JsonToolExecutor]. */
    private fun executeJsonTool(task: String, selectedTools: List<Tool>): String {
        // TODO - remove dependence on specific OpenAI adapter
        val exec = JsonToolExecutor(
            OpenAiAdapter.Companion.INSTANCE,
            controller.chatService.value.modelId,
            selectedTools.map { it.toJsonTool() })
        return runBlocking {
            exec.execute(task)
        }
    }

    /** Executes using [tri.ai.tool.JsonMultimodalToolExecutor]. */
    private fun executeJsonMultimodalTool(task: String, selectedTools: List<Tool>): String {
        val exec = JsonMultimodalToolExecutor(model.value!!, selectedTools.map { it.toJsonTool() })
        return runBlocking {
            exec.execute(task)
        }
    }

    /** Executes using [tri.ai.tool.wf.WorkflowExecutor]. */
    private fun executeWorkflowPlanner(task: String, selectedTools: List<Tool>): String {
        val exec = WorkflowExecutor(
            WExecutorChat(controller.completionEngine.value, maxTokens = 2000, temp = 0.5),
            selectedTools.map { it.toSolver() }
        )
        val request = WorkflowUserRequest(task)
        return exec.solve(request).finalResult().toString()
    }

    //endregion

    /** Executes a view's tool with specified input. Switches to show the view while executing. */
    private fun executeTask(view: AiTaskView, input: ToolDict): ToolResult {
        val result = CompletableDeferred<String>()
        runLater {
            pfxWorkspace.dock(view)
            tools.onEach {
                if (it.state == ToolState.ACTIVE)
                    it.state = ToolState.USED
            }
            tools.firstOrNull { it.name == view.title }?.let {
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

        /** Converts a [Tool] to a [tri.ai.tool.JsonTool]. */
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

        /** Converts a [Tool] to a [tri.ai.tool.wf.WorkflowSolver]. */
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
        private abstract class SelectableTool(name: String, description: String, val category: String): Tool(name, description, requiresLlm = true) {
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