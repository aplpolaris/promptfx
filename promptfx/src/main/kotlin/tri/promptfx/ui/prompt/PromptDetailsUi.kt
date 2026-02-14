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
package tri.promptfx.ui.prompt

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.prompt.PromptDef

/** View showing details of a prompt. */
class PromptDetailsUi : Fragment("Prompt") {

    var prompt = SimpleObjectProperty<PromptDef>().apply {
        onChange { contextTodayFlag.set(it?.contextInject?.today ?: true) }
    }
    private var contextTodayFlag = SimpleBooleanProperty(true)

    override val root = vbox {
        vgrow = Priority.ALWAYS
        scrollpane {
            prefViewportHeight = 800.0
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            isFitToWidth = true
            form {
                fieldset("Prompt Group(s)") {
                    field("Category") { text(prompt.stringBinding { it?.category ?: "N/A" }) }
                    field("Tags") {
                        text(prompt.stringBinding { it?.tags?.joinToString(", ") ?: "N/A" }) {
                            wrappingWidth = 400.0
                        }
                    }
                }
                fieldset("Prompt Metadata") {
                    field("Id") { text(prompt.stringBinding { it?.id ?: "N/A" }) }
                    field("(Bare Id)") { text(prompt.stringBinding { it?.bareId ?: "N/A" }) }
                    field("Version") { text(prompt.stringBinding { it?.version ?: "N/A" }) }
                    field("Name") { text(prompt.stringBinding { it?.name ?: "N/A" }) }
                    field("Title") { text(prompt.stringBinding { it?.title ?: "N/A" }) }
                    field("Description") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(prompt.stringBinding { it?.description ?: "N/A" }) {
                            wrappingWidth = 400.0
                        }
                    }
                    field("Arguments") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(prompt.stringBinding {
                            it?.args?.joinToString("\n") { arg ->
                                if (arg.description.isNullOrBlank())
                                    "${arg.name} (${arg.type})"
                                else
                                    "${arg.name} (${arg.type}): ${arg.description}"
                            } ?: "N/A"
                        }) {
                            wrappingWidth = 400.0
                        }
                    }

                }
                fieldset("Template") {
                    field("Context") {
                        checkbox("Inject current date", contextTodayFlag)
                    }
                    field("Template") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(prompt.stringBinding { it?.template ?: "" }) {
                            hgrow = Priority.ALWAYS
                            prefWidth = 0.0
                            wrappingWidth = 400.0
                        }
                    }
                }
            }
        }
    }

}
