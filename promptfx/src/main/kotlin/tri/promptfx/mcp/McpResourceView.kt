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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import kotlinx.coroutines.*
import tornadofx.*
import tri.ai.mcp.McpResource
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import tri.util.warning

/** Plugin for the [McpResourceView]. */
class McpResourcePlugin : NavigableWorkspaceViewImpl<McpResourceView>("MCP", "MCP Resources", type = McpResourceView::class)

/** Data class to track resources with their server information. */
data class ResourceWithServer(
    val resource: McpResource,
    val serverName: String
)

/** View and try out MCP server resources. */
class McpResourceView : AiTaskView("MCP Resources", "View and test resources for configured MCP servers (resources/list, resources/read).") {

    private val mcpController = controller.mcpController
    private val resourceEntries = observableListOf<ResourceWithServer>()
    private var resourceFilter: (String) -> Boolean = { true }
    private val filteredResourceEntries = observableListOf<ResourceWithServer>()
    private val resourceSelection = SimpleObjectProperty<ResourceWithServer>()
    private lateinit var resourceListView: ListView<ResourceWithServer>

    private val resourceContentText = SimpleStringProperty("")
    
    companion object {
        private const val RESOURCE_LOAD_TIMEOUT_MS = 10000L
    }

    init {
        // Load resources initially
        loadResources()
        
        input {
            toolbar {
                textfield("") {
                    promptText = "Search"
                    setOnKeyPressed {
                        resourceFilter = { text in it }
                        refilter()
                    }
                }
                spacer()
                button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                    tooltip("Refresh the resource list.")
                    action {
                        loadResources()
                    }
                }
            }
            resourceListView = listview(filteredResourceEntries) {
                vgrow = Priority.ALWAYS
                resourceSelection.bind(this.selectionModel.selectedItemProperty())
                cellFormat {
                    text = "${it.resource.name} [${it.serverName}]"
                    tooltip(it.resource.description ?: it.resource.uri)
                }
            }
        }
        outputPane.clear()
        output {
            scrollpane {
                isFitToWidth = true
                form {
                    visibleWhen(resourceSelection.isNotNull)
                    managedWhen(visibleProperty())
                    vgrow = Priority.ALWAYS
                    
                    fieldset("MCP Server Information") {
                        field("Server Id") {
                            label(resourceSelection.stringBinding { it?.serverName ?: "" })
                        }
                    }
                    
                    fieldset("Resource Metadata") {
                        field("URI") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            text(resourceSelection.stringBinding { it?.resource?.uri ?: "" }) {
                                wrappingWidth = 400.0
                            }
                        }
                        field("Name") {
                            label(resourceSelection.stringBinding { it?.resource?.name ?: "" })
                        }
                        field("Title") {
                            label(resourceSelection.stringBinding { it?.resource?.title ?: "" })
                        }
                        field("Description") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            text(resourceSelection.stringBinding { it?.resource?.description ?: "N/A" }) {
                                wrappingWidth = 400.0
                            }
                        }
                        field("MIME Type") {
                            label(resourceSelection.stringBinding { it?.resource?.mimeType ?: "N/A" })
                        }
                    }
                    
                    fieldset("Read Resource") {
                        hbox(alignment = Pos.CENTER_LEFT) {
                            button("Read Resource", FontAwesomeIcon.PLAY.graphic) {
                                action { readResourceContent() }
                            }
                        }
                    }

                    fieldset("Response") {
                        visibleWhen(resourceContentText.isNotEmpty)
                        field("Content") {
                            labelContainer.alignment = Pos.TOP_LEFT
                            textarea(resourceContentText) {
                                isEditable = false
                                isWrapText = true
                                prefRowCount = 12
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

    private fun readResourceContent() {
        val resourceWithServer = resourceSelection.value ?: return
        val resource = resourceWithServer.resource
        val serverName = resourceWithServer.serverName

        runAsync {
            try {
                val server = mcpController.mcpProviderRegistry.getProvider(serverName)
                    ?: return@runAsync "Error: Server '$serverName' not found"
                
                val response = runBlocking {
                    server.readResource(resource.uri)
                }
                
                // Format the response content
                val contentLines = response.contents.mapIndexed { index, content ->
                    buildString {
                        if (response.contents.size > 1) {
                            appendLine("--- Content ${index + 1} ---")
                        }
                        when {
                            content.text != null -> append(content.text)
                            content.blob != null -> append("[Binary content (base64)]\n${content.blob}")
                            else -> append("[Empty content]")
                        }
                    }
                }
                
                contentLines.joinToString("\n\n")
            } catch (e: Exception) {
                "Error reading resource: ${e.message}"
            }
        } ui { result ->
            resourceContentText.value = result
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        return AiPipelineResult.todo()
    }

    private fun loadResources() {
        // Clear existing entries before loading new ones
        resourceEntries.clear()
        
        val serverNames = mcpController.mcpProviderRegistry.listProviderNames()
        
        // Launch a separate coroutine for each server to load resources concurrently
        serverNames.forEach { serverName ->
            runAsync {
                try {
                    runBlocking {
                        withTimeout(RESOURCE_LOAD_TIMEOUT_MS) {
                            val server = mcpController.mcpProviderRegistry.getProvider(serverName)
                            if (server != null) {
                                val resources = server.listResources()
                                resources.map { ResourceWithServer(it, serverName) }
                            } else {
                                emptyList()
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    warning<McpResourceView>("Timeout loading resources from server '$serverName' after ${RESOURCE_LOAD_TIMEOUT_MS}ms")
                    emptyList()
                } catch (e: Exception) {
                    warning<McpResourceView>("Failed to load resources from server '$serverName': ${e.message}")
                    emptyList()
                }
            } ui { resources ->
                // Update the observable list incrementally as each server responds
                resourceEntries.addAll(resources)
                refilter()
            }
        }
    }

    private fun refilter() {
        filteredResourceEntries.setAll(resourceEntries.filter { resourceFilter(it.resource.name) })
    }
}
