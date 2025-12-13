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
package tri.promptfx.mcp

import com.fasterxml.jackson.databind.JsonNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiTaskView
import tri.util.json.jsonMapper
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [McpToolView]. */
class McpToolPlugin : NavigableWorkspaceViewImpl<McpToolView>("MCP", "Tools", type = McpToolView::class)

/** View and try out MCP server tools. */
class McpToolView : AiTaskView("MCP Tools", "View and test tools for configured MCP servers.") {

    private val mcpController = controller.mcpController
    private val toolEntries = observableListOf<Executable>()
    private val toolSelection = SimpleObjectProperty<Executable>()
    private lateinit var toolListView: ListView<Executable>

    private val toolInputText = SimpleStringProperty("{}")
    private val toolOutputText = SimpleStringProperty("")

    init {
        // Load tools initially
        loadTools()
        
        input {
            toolbar {
                button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                    tooltip("Refresh the tool list.")
                    action {
                        loadTools()
                    }
                }
            }
            toolListView = listview(toolEntries) {
                vgrow = Priority.ALWAYS
                toolSelection.bind(this.selectionModel.selectedItemProperty())
                cellFormat {
                    text = it.name
                    tooltip(it.description)
                }
            }
        }
        outputPane.clear()
        output {
            toolbar {
                label("Tool Details")
            }
            form {
                visibleWhen(toolSelection.isNotNull)
                managedWhen(visibleProperty())
                fieldset("Tool Information") {
                    field("Name") {
                        label(toolSelection.stringBinding { it?.name ?: "" })
                    }
                    field("Description") {
                        textarea {
                            isEditable = false
                            isWrapText = true
                            prefRowCount = 2
                            textProperty().bind(toolSelection.stringBinding { it?.description ?: "" })
                        }
                    }
                    field("Version") {
                        label(toolSelection.stringBinding { it?.version ?: "" })
                    }
                }
                fieldset("Input Schema") {
                    textarea {
                        isEditable = false
                        isWrapText = true
                        prefRowCount = 5
                        textProperty().bind(toolSelection.stringBinding { 
                            it?.inputSchema?.toPrettyString() ?: "No schema available" 
                        })
                    }
                }
                fieldset("Output Schema") {
                    textarea {
                        isEditable = false
                        isWrapText = true
                        prefRowCount = 5
                        textProperty().bind(toolSelection.stringBinding { 
                            it?.outputSchema?.toPrettyString() ?: "No schema available" 
                        })
                    }
                }
            }
            separator()
            toolbar {
                label("Try Tool")
            }
            form {
                visibleWhen(toolSelection.isNotNull)
                managedWhen(visibleProperty())
                fieldset("Input Parameters (JSON)") {
                    textarea(toolInputText) {
                        isWrapText = true
                        prefRowCount = 5
                        promptText = """{"param1": "value1"}"""
                    }
                }
                buttonbar {
                    button("Execute Tool") {
                        enableWhen(toolSelection.isNotNull)
                        action { runToolExecution() }
                    }
                }
                fieldset("Output") {
                    textarea(toolOutputText) {
                        isEditable = false
                        isWrapText = true
                        prefRowCount = 8
                        vgrow = Priority.ALWAYS
                    }
                }
            }
        }
        hideParameters()
        hideRunButton()
    }

    private fun runToolExecution() {
        val tool = toolSelection.value ?: return
        val inputJson = toolInputText.value

        runAsync {
            try {
                val inputNode: JsonNode = jsonMapper.readTree(inputJson)
                // Note: runAsync already provides coroutine context, but tool.execute is suspend
                // We need to use runBlocking here because runAsync uses a different threading model
                val output = kotlinx.coroutines.runBlocking {
                    tool.execute(inputNode, ExecContext())
                }
                "Success:\n${output.toPrettyString()}"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        } ui { result ->
            toolOutputText.value = result
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        return AiPipelineResult(
            AiPromptTrace.error(null, "Not implemented", null),
            mapOf()
        )
    }

    private fun loadTools() {
        runAsync {
            val allTools = mutableListOf<Executable>()
            for (serverName in mcpController.mcpServerRegistry.listServerNames()) {
                try {
                    val server = mcpController.mcpServerRegistry.getServer(serverName)
                    if (server != null) {
                        val tools = kotlinx.coroutines.runBlocking {
                            server.listTools()
                        }
                        allTools.addAll(tools)
                    }
                } catch (e: Exception) {
                    println("Error loading tools from server $serverName: ${e.message}")
                }
            }
            allTools
        } ui { tools ->
            toolEntries.setAll(tools)
        }
    }
}
