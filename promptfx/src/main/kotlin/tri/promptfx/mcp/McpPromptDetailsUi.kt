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

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

/** View showing details of an MCP prompt. */
class McpPromptDetailsUi : Fragment("MCP Prompt") {

    var promptWithServer = SimpleObjectProperty<McpPromptWithServer>()

    override val root = vbox {
        vgrow = Priority.ALWAYS
        scrollpane {
            prefViewportHeight = 800.0
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            isFitToWidth = true
            form {
                fieldset("Server Information") {
                    field("Server") { 
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
            }
        }
    }
}
