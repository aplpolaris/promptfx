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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.mcp.registry.EmbeddedServerConfig
import tri.ai.mcp.registry.HttpServerConfig
import tri.ai.mcp.registry.McpServerConfig
import tri.ai.mcp.registry.StdioServerConfig
import tri.ai.mcp.registry.TestServerConfig
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxMcpController
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [McpServerView]. */
class McpServerPlugin : NavigableWorkspaceViewImpl<McpServerView>("MCP", "MCP Servers", type = McpServerView::class)

/** Data class to hold server capability information. */
data class ServerCapabilityInfo(
    val hasPrompts: Boolean = false,
    val hasTools: Boolean = false,
    val hasResources: Boolean = false,
    val promptsCount: Int = 0,
    val toolsCount: Int = 0,
    val resourcesCount: Int = 0,
    val error: String? = null
)

/** View and try out MCP server prompts. */
class McpServerView : AiTaskView("MCP Servers", "View and configure MCP Servers.") {

    private val mcpController: PromptFxMcpController by inject()
    private val serverNames = observableListOf<String>()
    private val selectedServerName = SimpleObjectProperty<String>()
    private val selectedServerConfig = SimpleObjectProperty<McpServerConfig>()
    private val serverCapabilities = mutableMapOf<String, SimpleObjectProperty<ServerCapabilityInfo>>()
    
    private fun getCapabilityInfo(serverName: String): SimpleObjectProperty<ServerCapabilityInfo> {
        return serverCapabilities.getOrPut(serverName) {
            SimpleObjectProperty(ServerCapabilityInfo())
        }
    }

    init {
        // Load server names from registry
        serverNames.setAll(mcpController.mcpServerRegistry.listServerNames())

        // Update config when server is selected
        selectedServerName.onChange { serverName ->
            selectedServerConfig.value = serverName?.let {
                mcpController.mcpServerRegistry.getConfigs()[it]
            }
            // Query capabilities when server is selected
            serverName?.let { queryServerCapabilities(it) }
        }

        hideParameters()
        hideRunButton()

        outputPane.clear()
        input {
            // Left pane: Server list
            vbox {
                vgrow = Priority.ALWAYS
                toolbar {
                    label("Servers")
                    spacer()
                    button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                        tooltip("Refresh server list")
                        action {
                            serverNames.setAll(mcpController.mcpServerRegistry.listServerNames())
                        }
                    }
                }
                listview(serverNames) {
                    vgrow = Priority.ALWAYS
                    selectedServerName.bind(this.selectionModel.selectedItemProperty())
                    cellFormat { serverName ->
                        graphic = hbox(spacing = 5.0) {
                            label(serverName) {
                                minWidth = 100.0
                            }
                            spacer()
                            
                            val capInfo = getCapabilityInfo(serverName)
                            
                            // Show icons for supported capabilities
                            hbox(spacing = 3.0) {
                                visibleWhen(capInfo.booleanBinding { 
                                    it != null && (it.hasPrompts || it.hasTools || it.hasResources) 
                                })
                                
                                // Prompts icon
                                label {
                                    visibleWhen(capInfo.booleanBinding { it?.hasPrompts == true })
                                    graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT).apply {
                                        glyphSize = 12
                                        fill = Color.DODGERBLUE
                                    }
                                    tooltip("Has prompts")
                                }
                                
                                // Tools icon
                                label {
                                    visibleWhen(capInfo.booleanBinding { it?.hasTools == true })
                                    graphic = FontAwesomeIconView(FontAwesomeIcon.WRENCH).apply {
                                        glyphSize = 12
                                        fill = Color.ORANGE
                                    }
                                    tooltip("Has tools")
                                }
                                
                                // Resources icon
                                label {
                                    visibleWhen(capInfo.booleanBinding { it?.hasResources == true })
                                    graphic = FontAwesomeIconView(FontAwesomeIcon.DATABASE).apply {
                                        glyphSize = 12
                                        fill = Color.GREEN
                                    }
                                    tooltip("Has resources")
                                }
                            }
                        }
                    }
                }
            }
        }

        output {
            // Right pane: Server details
            vbox {
                vgrow = Priority.ALWAYS
                padding = insets(10.0)
                spacing = 10.0

                visibleWhen(selectedServerName.isNotNull)
                managedWhen(visibleProperty())

                label {
                    textProperty().bind(selectedServerName)
                    style {
                        fontSize = 18.px
                        fontWeight = FontWeight.BOLD
                    }
                }

                form {
                    fieldset("Server Configuration") {
                        field("Type") {
                            label {
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    when (it) {
                                        is EmbeddedServerConfig -> "Embedded"
                                        is StdioServerConfig -> "Stdio"
                                        is HttpServerConfig -> "HTTP"
                                        is TestServerConfig -> "Test"
                                        null -> ""
                                    }
                                })
                            }
                        }

                        field("Description") {
                            label {
                                isWrapText = true
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    it?.description ?: "No description available"
                                })
                            }
                        }
                    }

                    // Capabilities information
                    fieldset("Capabilities") {
                        val capInfo = selectedServerName.objectBinding { name ->
                            name?.let { getCapabilityInfo(it).value }
                        }
                        
                        field("Prompts") {
                            hbox(spacing = 5.0) {
                                label {
                                    graphic = FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT).apply {
                                        glyphSize = 14
                                        fill = Color.DODGERBLUE
                                    }
                                }
                                label {
                                    textProperty().bind(capInfo.stringBinding {
                                        when {
                                            it?.error != null -> "Error"
                                            it?.hasPrompts == true -> "Supported (${it.promptsCount} prompts)"
                                            else -> "Not supported"
                                        }
                                    })
                                }
                            }
                        }
                        
                        field("Tools") {
                            hbox(spacing = 5.0) {
                                label {
                                    graphic = FontAwesomeIconView(FontAwesomeIcon.WRENCH).apply {
                                        glyphSize = 14
                                        fill = Color.ORANGE
                                    }
                                }
                                label {
                                    textProperty().bind(capInfo.stringBinding {
                                        when {
                                            it?.error != null -> "Error"
                                            it?.hasTools == true -> "Supported (${it.toolsCount} tools)"
                                            else -> "Not supported"
                                        }
                                    })
                                }
                            }
                        }
                        
                        field("Resources") {
                            hbox(spacing = 5.0) {
                                label {
                                    graphic = FontAwesomeIconView(FontAwesomeIcon.DATABASE).apply {
                                        glyphSize = 14
                                        fill = Color.GREEN
                                    }
                                }
                                label {
                                    textProperty().bind(capInfo.stringBinding {
                                        when {
                                            it?.error != null -> "Error"
                                            it?.hasResources == true -> "Supported (${it.resourcesCount} resources)"
                                            else -> "Not supported"
                                        }
                                    })
                                }
                            }
                        }
                        
                        // Show error message if there was an issue querying capabilities
                        field("Status") {
                            visibleWhen(capInfo.booleanBinding { it?.error != null })
                            managedWhen(visibleProperty())
                            label {
                                isWrapText = true
                                style {
                                    textFill = Color.RED
                                }
                                textProperty().bind(capInfo.stringBinding { it?.error ?: "" })
                            }
                        }
                    }

                    // Type-specific configuration details
                    fieldset {
                        textProperty.bind(selectedServerConfig.stringBinding { "Details" })

                        // Embedded Server Config
                        field("Prompt Library Path") {
                            visibleWhen(selectedServerConfig.booleanBinding { it is EmbeddedServerConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? EmbeddedServerConfig)?.promptLibraryPath ?: "(default)"
                                })
                            }
                        }

                        // Stdio Server Config
                        field("Command") {
                            visibleWhen(selectedServerConfig.booleanBinding { it is StdioServerConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? StdioServerConfig)?.command ?: ""
                                })
                            }
                        }

                        field("Arguments") {
                            visibleWhen(selectedServerConfig.booleanBinding { it is StdioServerConfig })
                            managedWhen(visibleProperty())
                            label {
                                isWrapText = true
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? StdioServerConfig)?.args?.joinToString(" ") ?: ""
                                })
                            }
                        }

                        field("Environment") {
                            visibleWhen(selectedServerConfig.booleanBinding {
                                it is StdioServerConfig && it.env.isNotEmpty()
                            })
                            managedWhen(visibleProperty())
                            label {
                                isWrapText = true
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? StdioServerConfig)?.env?.entries?.joinToString("\n") { "${it.key}=${it.value}" }
                                        ?: ""
                                })
                            }
                        }

                        // HTTP Server Config
                        field("URL") {
                            visibleWhen(selectedServerConfig.booleanBinding { it is HttpServerConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? HttpServerConfig)?.url ?: ""
                                })
                            }
                        }

                        // Test Server Config
                        field("Include Default Prompts") {
                            visibleWhen(selectedServerConfig.booleanBinding { it is TestServerConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? TestServerConfig)?.includeDefaultPrompts?.toString() ?: ""
                                })
                            }
                        }

                        field("Include Default Tools") {
                            visibleWhen(selectedServerConfig.booleanBinding { it is TestServerConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? TestServerConfig)?.includeDefaultTools?.toString() ?: ""
                                })
                            }
                        }

                        field("Include Default Resources") {
                            visibleWhen(selectedServerConfig.booleanBinding { it is TestServerConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedServerConfig.stringBinding {
                                    (it as? TestServerConfig)?.includeDefaultResources?.toString() ?: ""
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun queryServerCapabilities(serverName: String) {
        val capabilityProp = getCapabilityInfo(serverName)
        
        // Run query in background using coroutine
        runAsync {
            runBlocking {
                try {
                    val adapter = mcpController.mcpServerRegistry.getServer(serverName)
                        ?: return@runBlocking ServerCapabilityInfo(error = "Server not found")
                    
                    val capabilities = adapter.getCapabilities()
                    
                    // Only query for counts if the capability is supported
                    val promptsCount = if (capabilities?.prompts != null) {
                        try {
                            adapter.listPrompts().size
                        } catch (e: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                    
                    val toolsCount = if (capabilities?.tools != null) {
                        try {
                            adapter.listTools().size
                        } catch (e: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                    
                    // Resources API is not yet implemented in the adapter interface
                    val resourcesCount = 0
                    
                    ServerCapabilityInfo(
                        hasPrompts = capabilities?.prompts != null,
                        hasTools = capabilities?.tools != null,
                        hasResources = capabilities?.resources != null,
                        promptsCount = promptsCount,
                        toolsCount = toolsCount,
                        resourcesCount = resourcesCount
                    )
                } catch (e: Exception) {
                    ServerCapabilityInfo(error = "Error: ${e.message}")
                }
            }
        } ui { result ->
            capabilityProp.value = result
        }
    }

    override suspend fun processUserInput() = AiPipelineResult.todo()
}
