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
import javafx.scene.text.FontWeight
import tornadofx.*
import tri.ai.mcp.*
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxMcpController
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [McpServerView]. */
class McpServerPlugin : NavigableWorkspaceViewImpl<McpServerView>("MCP", "MCP Servers", type = McpServerView::class)

/** View and try out MCP server prompts. */
class McpServerView : AiTaskView("MCP Servers", "View and configure MCP Servers.") {
    
    private val mcpController: PromptFxMcpController by inject()
    private val serverNames = observableListOf<String>()
    private val selectedServerName = SimpleObjectProperty<String>()
    private val selectedServerConfig = SimpleObjectProperty<McpServerConfig>()
    
    init {
        // Load server names from registry
        serverNames.setAll(mcpController.mcpServerRegistry.listServerNames())
        
        // Update config when server is selected
        selectedServerName.onChange { serverName ->
            selectedServerConfig.value = serverName?.let { 
                mcpController.mcpServerRegistry.getConfigs()[it] 
            }
        }
        
        hideParameters()
        hideRunButton()
        
        outputPane.clear()
        output {
            splitpane {
                vgrow = Priority.ALWAYS
                
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
                        cellFormat {
                            text = it
                        }
                    }
                }
                
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
                                            else -> it::class.simpleName ?: "Unknown"
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
                        
                        // Type-specific configuration details
                        fieldset {
                            textProperty().bind(selectedServerConfig.stringBinding { "Details" })
                            
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
                                    it is StdioServerConfig && (it as StdioServerConfig).env.isNotEmpty() 
                                })
                                managedWhen(visibleProperty())
                                label {
                                    isWrapText = true
                                    textProperty().bind(selectedServerConfig.stringBinding { 
                                        (it as? StdioServerConfig)?.env?.entries?.joinToString("\n") { "${it.key}=${it.value}" } ?: "" 
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
                        }
                    }
                }
            }
        }
    }
    
    override suspend fun processUserInput() = AiPipelineResult.todo()
}
