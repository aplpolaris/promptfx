/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.mcp

import com.fasterxml.jackson.module.kotlin.readValue
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import kotlinx.coroutines.*
import tornadofx.*
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.version
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiTaskView
import tri.util.json.jsonMapper
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import tri.util.warning

/** Plugin for the [McpToolView]. */
class McpToolPlugin : NavigableWorkspaceViewImpl<McpToolView>("MCP", "MCP Tools", type = McpToolView::class)

/** Data class to track tools with their server information. */
data class ToolWithServer(
    val tool: McpToolMetadata,
    val serverName: String
)

/** View and try out MCP server tools. */
class McpToolView : AiTaskView("MCP Tools", "View and call tools for configured MCP servers (tools/list, tools/call).") {

    private val mcpController = controller.mcpController
    private val toolEntries = observableListOf<ToolWithServer>()
    private var toolFilter: (String) -> Boolean = { true }
    private val filteredToolEntries = observableListOf<ToolWithServer>()
    private val toolSelection = SimpleObjectProperty<ToolWithServer>()
    private lateinit var toolListView: ListView<ToolWithServer>

    private val toolInputText = SimpleStringProperty("{}")
    private val outputContentText = SimpleStringProperty("")
    
    companion object {
        private const val TOOL_LOAD_TIMEOUT_MS = 10000L
    }

    init {
        // Load tools initially
        loadTools()
        
        input {
            toolbar {
                textfield("") {
                    promptText = "Search"
                    textProperty().addListener { _, _, newValue ->
                        toolFilter = { it.contains(newValue, ignoreCase = true) }
                        refilter()
                    }
                }
                spacer()
                button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                    tooltip("Refresh the tool list.")
                    action {
                        loadTools()
                    }
                }
            }
            toolListView = listview(filteredToolEntries) {
                vgrow = Priority.ALWAYS
                toolSelection.bind(this.selectionModel.selectedItemProperty())
                cellFormat {
                    text = "${it.tool.name} [${it.serverName}]"
                    tooltip(it.tool.description)
                }
            }
        }
        outputPane.clear()
        output {
            scrollpane {
                isFitToWidth = true
                form {
                    visibleWhen(toolSelection.isNotNull)
                    managedWhen(visibleProperty())
                    vgrow = Priority.ALWAYS
                    
                    fieldset("MCP Server Information") {
                        field("Server Id") {
                            label(toolSelection.stringBinding { it?.serverName ?: "" })
                        }
                    }
                    
                    fieldset("Tool Metdata") {
                        field("Name") {
                            label(toolSelection.stringBinding { it?.tool?.name ?: "" })
                        }
                        field("Title") {
                            label(toolSelection.stringBinding { it?.tool?.title ?: "" })
                        }
                        field("Description") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            text(toolSelection.stringBinding { it?.tool?.description ?: "N/A" }) {
                                wrappingWidth = 400.0
                            }
                        }
                        field("Version") {
                            label(toolSelection.stringBinding { it?.tool?.version ?: "" })
                        }
                        field("Input Schema") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            textarea {
                                isEditable = false
                                isWrapText = true
                                prefRowCount = 5
                                textProperty().bind(toolSelection.stringBinding {
                                    it?.tool?.inputSchema?.toPrettyString() ?: "No schema available"
                                })
                            }
                        }
                        field("Output Schema") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            textarea {
                                isEditable = false
                                isWrapText = true
                                prefRowCount = 5
                                textProperty().bind(toolSelection.stringBinding {
                                    it?.tool?.outputSchema?.toPrettyString() ?: "No schema available"
                                })
                            }
                        }
                    }
                    
                    fieldset("Call Tool") {
                        field("Parameters (JSON)") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            textarea(toolInputText) {
                                isWrapText = true
                                prefRowCount = 5
                                promptText = """{"param1": "value1"}"""
                            }
                        }
                        hbox(alignment = Pos.CENTER_LEFT) {
                            button("Call Tool", FontAwesomeIcon.PLAY.graphic) {
                                action { runToolExecution() }
                            }
                        }
                    }
                    fieldset("Response") {
                        visibleWhen(outputContentText.isNotEmpty)
                        field("Content") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            textarea(outputContentText) {
                                isEditable = false
                                isWrapText = true
                                prefRowCount = 8
                                vgrow = Priority.ALWAYS
                            }
                        }
                    }
                }
            }
        }
        hideParameters()
        hideRunButton()
    }

    private fun runToolExecution() {
        val toolWithServer = toolSelection.value ?: return
        val inputJson = toolInputText.value

        runAsync {
            try {
                val inputNode: Map<String, Any?> = jsonMapper.readValue<Map<String, Any?>>(inputJson)
                val output = runBlocking {
                    val server = mcpController.mcpProviderRegistry.getProvider(toolWithServer.serverName)
                        ?: throw IllegalStateException("MCP server '${toolWithServer.serverName}' not found")
                    val result = server.callTool(toolWithServer.tool.name, inputNode)
                    result
                }
                if (output.isError == true) {
                    "Error reported by MCP server: " + (output.errorMessage() ?: "Unknown")
                } else if (output.structuredContent != null) {
                    output.structuredContent!!.toPrettyString()
                } else {
                    output.content.joinToString("\n\n") {
                        jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(it)
                    }
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        } ui { result ->
            outputContentText.value = result
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        return AiPipelineResult(
            AiPromptTrace.error(null, "Not implemented", null),
            mapOf()
        )
    }

    private fun loadTools() {
        // Clear existing entries before loading new ones
        toolEntries.clear()
        
        val serverNames = mcpController.mcpProviderRegistry.listProviderNames()
        
        // Launch a separate coroutine for each server to load tools concurrently
        serverNames.forEach { serverName ->
            runAsync {
                try {
                    runBlocking {
                        withTimeout(TOOL_LOAD_TIMEOUT_MS) {
                            val server = mcpController.mcpProviderRegistry.getProvider(serverName)
                            if (server != null) {
                                val tools = server.listTools()
                                tools.map { ToolWithServer(it, serverName) }
                            } else {
                                emptyList()
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    warning<McpToolView>("Timeout loading tools from server '$serverName' after ${TOOL_LOAD_TIMEOUT_MS}ms")
                    emptyList()
                } catch (e: Exception) {
                    warning<McpToolView>("Failed to load tools from server '$serverName': ${e.message}")
                    emptyList()
                }
            } ui { tools ->
                // Update the observable list incrementally as each server responds
                toolEntries.addAll(tools)
                refilter()
            }
        }
    }

    private fun refilter() {
        filteredToolEntries.setAll(toolEntries.filter { toolFilter(it.tool.name) })
    }
}
