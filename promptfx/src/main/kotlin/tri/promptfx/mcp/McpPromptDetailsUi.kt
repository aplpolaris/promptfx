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
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tornadofx.*
import tri.ai.mcp.McpPromptResponse
import tri.promptfx.PromptFxMcpController

/** View showing details of an MCP prompt. */
class McpPromptDetailsUi : Fragment("MCP Prompt") {

    private val mcpController: PromptFxMcpController by inject()
    var promptWithServer = SimpleObjectProperty<McpPromptWithServer>()
    private val argumentValues = mutableMapOf<String, SimpleStringProperty>()
    private val promptResponse = SimpleObjectProperty<McpPromptResponse?>()

    override val root = vbox {
        vgrow = Priority.ALWAYS
        scrollpane {
            prefViewportHeight = 800.0
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            isFitToWidth = true
            form {
                fieldset("MCP Server Information") {
                    field("Server Id") { 
                        text(promptWithServer.stringBinding { it?.serverName ?: "N/A" })
                    }
                }
                fieldset("Prompt Metadata") {
                    field("Name") { 
                        text(promptWithServer.stringBinding { it?.prompt?.name ?: "N/A" }) 
                    }
                    field("Title") { 
                        text(promptWithServer.stringBinding { it?.prompt?.title ?: "N/A" }) 
                    }
                    field("Description") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(promptWithServer.stringBinding { it?.prompt?.description ?: "N/A" }) {
                            wrappingWidth = 400.0
                        }
                    }
                    field("Arguments") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(promptWithServer.stringBinding {
                            it?.prompt?.arguments?.joinToString("\n") { arg ->
                                val requiredStr = if (arg.required) " (required)" else " (optional)"
                                "${arg.name}$requiredStr: ${arg.description}"
                            } ?: "N/A"
                        }) {
                            wrappingWidth = 400.0
                        }
                    }
                }
                fieldset("Try Prompt") {
                    vbox {
                        spacing = 10.0
                        
                        // Dynamically create input fields for each argument
                        promptWithServer.onChange { pwsValue ->
                            children.clear()
                            argumentValues.clear()
                            promptResponse.set(null)
                            
                            pwsValue?.prompt?.arguments?.forEach { arg ->
                                val valueProperty = SimpleStringProperty("")
                                argumentValues[arg.name] = valueProperty
                                
                                hbox {
                                    spacing = 5.0
                                    label(arg.name) {
                                        minWidth = 100.0
                                        if (arg.required) {
                                            style = "-fx-font-weight: bold"
                                        }
                                    }
                                    textfield(valueProperty) {
                                        promptText = arg.description
                                        hgrow = Priority.ALWAYS
                                    }
                                }
                            }
                            
                            // Add execute button
                            hbox {
                                spacing = 10.0
                                alignment = Pos.CENTER_LEFT
                                paddingTop = 10.0
                                
                                button("Get Filled Prompt", FontAwesomeIconView(FontAwesomeIcon.PLAY)) {
                                    action {
                                        executePrompt()
                                    }
                                }
                            }
                        }
                    }
                }
                fieldset("Prompt Result") {
                    visibleWhen(promptResponse.isNotNull)
                    managedWhen(visibleProperty())
                    
                    field("Description") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(promptResponse.stringBinding { it?.description ?: "N/A" }) {
                            wrappingWidth = 400.0
                        }
                    }
                    
                    // Dynamically create a field for each message
                    vbox {
                        spacing = 10.0
                        promptResponse.onChange { response ->
                            children.clear()
                            response?.messages?.forEachIndexed { index, message ->
                                form {
                                    fieldset("Message ${index + 1}") {
                                        field("Role") {
                                            label(message.role.toString())
                                        }
                                        field("Content") {
                                            labelContainer.alignment = Pos.TOP_LEFT
                                            vbox {
                                                spacing = 5.0
                                                message.content?.forEach { part ->
                                                    textarea(part?.text ?: "") {
                                                        isEditable = false
                                                        isWrapText = true
                                                        prefRowCount = 5
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun executePrompt() {
        val pws = promptWithServer.value ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val server = mcpController.mcpProviderRegistry.getProvider(pws.serverName)
                if (server != null) {
                    val args = argumentValues.mapValues { it.value.value }
                    val response = server.getPrompt(pws.prompt.name, args)
                    
                    Platform.runLater {
                        promptResponse.set(response)
                    }
                } else {
                    Platform.runLater {
                        // Create an error response
                        promptResponse.set(McpPromptResponse(
                            description = "Error: Server '${pws.serverName}' not found",
                            messages = emptyList()
                        ))
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    // Create an error response
                    promptResponse.set(McpPromptResponse(
                        description = "Error executing prompt: ${e.message}",
                        messages = emptyList()
                    ))
                }
            }
        }
    }
}
