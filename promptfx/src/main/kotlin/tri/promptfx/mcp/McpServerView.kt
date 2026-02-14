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
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.mcp.EmbeddedProviderConfig
import tri.ai.mcp.HttpProviderConfig
import tri.ai.mcp.McpProviderConfig
import tri.ai.mcp.StdioProviderConfig
import tri.ai.mcp.TestProviderConfig
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxMcpController
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [McpServerView]. */
class McpServerPlugin : NavigableWorkspaceViewImpl<McpServerView>("MCP", "MCP Servers", type = McpServerView::class)

/** Data class to hold provider capability information. */
data class ProviderCapabilityInfo(
    val loading: Boolean = false,
    val hasPrompts: Boolean = false,
    val hasTools: Boolean = false,
    val hasResources: Boolean = false,
    val promptsCount: Int = 0,
    val toolsCount: Int = 0,
    val resourcesCount: Int = 0,
    val resourceTemplatesCount: Int = 0,
    val error: String? = null
)

/** View to show registered MCP providers. */
class McpServerView : AiTaskView("MCP Servers", "View and configure MCP Servers.") {

    private val mcpController: PromptFxMcpController by inject()
    private val providerNames = observableListOf<String>()
    private val selectedProviderName = SimpleObjectProperty<String>()
    private val selectedConfig = SimpleObjectProperty<McpProviderConfig>()
    private val providerCapabilities = mutableMapOf<String, SimpleObjectProperty<ProviderCapabilityInfo>>()
    
    private fun getCapabilityInfo(name: String): SimpleObjectProperty<ProviderCapabilityInfo> {
        return providerCapabilities.getOrPut(name) {
            SimpleObjectProperty(ProviderCapabilityInfo())
        }
    }

    init {
        // Load names from registry
        providerNames.setAll(mcpController.mcpProviderRegistry.listProviderNames())

        // Update config when server is selected
        selectedProviderName.onChange { name ->
            selectedConfig.value = name?.let {
                mcpController.mcpProviderRegistry.getConfigs()[it]
            }
            // Query capabilities when server is selected
            name?.let { queryServerCapabilities(it) }
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
                            providerNames.setAll(mcpController.mcpProviderRegistry.listProviderNames())
                        }
                    }
                }
                listview(providerNames) {
                    vgrow = Priority.ALWAYS
                    selectedProviderName.bind(this.selectionModel.selectedItemProperty())
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

                visibleWhen(selectedProviderName.isNotNull)
                managedWhen(visibleProperty())

                label {
                    textProperty().bind(selectedProviderName)
                    style {
                        fontSize = 18.px
                        fontWeight = FontWeight.BOLD
                    }
                }

                form {
                    fieldset("Server Configuration") {
                        field("Type") {
                            label {
                                textProperty().bind(selectedConfig.stringBinding {
                                    when (it) {
                                        is EmbeddedProviderConfig -> "Embedded"
                                        is StdioProviderConfig -> "Stdio"
                                        is HttpProviderConfig -> "HTTP"
                                        is TestProviderConfig -> "Test"
                                        null -> ""
                                    }
                                })
                            }
                        }

                        field("Description") {
                            label {
                                isWrapText = true
                                textProperty().bind(selectedConfig.stringBinding {
                                    it?.description ?: "No description available"
                                })
                            }
                        }
                    }

                    // Capabilities information
                    fieldset("Capabilities") {
                        val capInfo = selectedProviderName.objectBinding { name ->
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
                                            it?.loading == true -> "Loading..."
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
                                            it?.loading == true -> "Loading..."
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
                                            it?.loading == true -> "Loading..."
                                            it?.error != null -> "Error"
                                            it?.hasResources == true -> "Supported (${it.resourcesCount} resources, ${it.resourceTemplatesCount} templates)"
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
                        textProperty.bind(selectedConfig.stringBinding { "Details" })

                        // Embedded Server Config
                        field("Prompt Library Path") {
                            visibleWhen(selectedConfig.booleanBinding { it is EmbeddedProviderConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? EmbeddedProviderConfig)?.promptLibraryPath ?: "(default)"
                                })
                            }
                        }

                        // Stdio Server Config
                        field("Command") {
                            visibleWhen(selectedConfig.booleanBinding { it is StdioProviderConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? StdioProviderConfig)?.command ?: ""
                                })
                            }
                        }

                        field("Arguments") {
                            visibleWhen(selectedConfig.booleanBinding { it is StdioProviderConfig })
                            managedWhen(visibleProperty())
                            label {
                                isWrapText = true
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? StdioProviderConfig)?.args?.joinToString(" ") ?: ""
                                })
                            }
                        }

                        field("Environment") {
                            visibleWhen(selectedConfig.booleanBinding {
                                it is StdioProviderConfig && it.env.isNotEmpty()
                            })
                            managedWhen(visibleProperty())
                            label {
                                isWrapText = true
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? StdioProviderConfig)?.env?.entries?.joinToString("\n") { "${it.key}=${it.value}" }
                                        ?: ""
                                })
                            }
                        }

                        // HTTP Server Config
                        field("URL") {
                            visibleWhen(selectedConfig.booleanBinding { it is HttpProviderConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? HttpProviderConfig)?.url ?: ""
                                })
                            }
                        }

                        // Test Server Config
                        field("Include Default Prompts") {
                            visibleWhen(selectedConfig.booleanBinding { it is TestProviderConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? TestProviderConfig)?.includeDefaultPrompts?.toString() ?: ""
                                })
                            }
                        }

                        field("Include Default Tools") {
                            visibleWhen(selectedConfig.booleanBinding { it is TestProviderConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? TestProviderConfig)?.includeDefaultTools?.toString() ?: ""
                                })
                            }
                        }

                        field("Include Default Resources") {
                            visibleWhen(selectedConfig.booleanBinding { it is TestProviderConfig })
                            managedWhen(visibleProperty())
                            label {
                                textProperty().bind(selectedConfig.stringBinding {
                                    (it as? TestProviderConfig)?.includeDefaultResources?.toString() ?: ""
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
        
        // Set loading state immediately
        capabilityProp.value = ProviderCapabilityInfo(loading = true)
        
        // Run query in background using coroutine
        runAsync {
            runBlocking {
                try {
                    val provider = mcpController.mcpProviderRegistry.getProvider(serverName)
                        ?: return@runBlocking ProviderCapabilityInfo(error = "Server not found")
                    
                    val capabilities = provider.getCapabilities()
                    
                    // Only query for counts if the capability is supported
                    val promptsCount = if (capabilities?.prompts != null) {
                        try {
                            provider.listPrompts().size
                        } catch (e: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                    
                    val toolsCount = if (capabilities?.tools != null) {
                        try {
                            provider.listTools().size
                        } catch (e: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                    
                    val resourcesCount = if (capabilities?.resources != null) {
                        try {
                            provider.listResources().size
                        } catch (e: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                    
                    val resourceTemplatesCount = if (capabilities?.resources != null) {
                        try {
                            provider.listResourceTemplates().size
                        } catch (e: Exception) {
                            0
                        }
                    } else {
                        0
                    }
                    
                    ProviderCapabilityInfo(
                        hasPrompts = capabilities?.prompts != null,
                        hasTools = capabilities?.tools != null,
                        hasResources = capabilities?.resources != null,
                        promptsCount = promptsCount,
                        toolsCount = toolsCount,
                        resourcesCount = resourcesCount,
                        resourceTemplatesCount = resourceTemplatesCount
                    )
                } catch (e: Exception) {
                    ProviderCapabilityInfo(error = "Error: ${e.message}")
                }
            }
        } ui { result ->
            capabilityProp.value = result
        }
    }

    override suspend fun processUserInput() = AiPipelineResult.todo()
}
