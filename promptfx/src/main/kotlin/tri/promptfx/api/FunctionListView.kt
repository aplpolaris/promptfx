/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.api

import com.aallam.openai.api.chat.ChatCompletionFunction
import com.aallam.openai.api.chat.Parameters
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import kotlinx.serialization.SerializationException
import tornadofx.*
import tri.util.ifNotBlank

class FunctionListView : Fragment() {

    val components = observableListOf<FunctionLineModel>()

    override val root = vbox {
        vgrow = Priority.ALWAYS
        spacing = 10.0
        listview(components) {
            isFillWidth = true
            cellFormat {
                graphic = vbox {
                    spacing = 10.0
                    hbox {
                        spacing = 10.0
                        alignment = Pos.CENTER
                        text("Name")
                        textfield(it.nameProperty) {
                            hgrow = Priority.ALWAYS
                            prefColumnCount = 20
                        }
                        tooltip("The name of the function to be called. Must be a-z, A-Z, 0-9, or contain underscores and dashes, with a maximum length of 64.")
                        button("", FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE)) {
                            action { components.remove(it) }
                        }
                    }
                    hbox {
                        spacing = 10.0
                        alignment = Pos.CENTER
                        text("Description")
                        textfield(it.descriptionProperty) { hgrow = Priority.ALWAYS }
                        tooltip("Optional description of function")
                    }
                    hbox {
                        spacing = 10.0
                        alignment = Pos.TOP_LEFT
                        text("Parameters")
                        textarea(it.parametersProperty) {
                            tooltip("""The parameters the functions accepts, described as a JSON Schema object. To describe a function that accepts no parameters, provide the value {"type":"object", "properties": {}}.""")
                            prefRowCount = 3
                            isWrapText = true
                            hgrow = Priority.ALWAYS
                        }
                    }
                    prefWidth = 0.0
                }
            }
        }
        hbox {
            spacing = 10.0
            button("Add function", FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE)) {
                action { components.add(FunctionLineModel("function")) }
            }
            button("(for example)", FontAwesomeIconView(FontAwesomeIcon.SUN_ALT)) {
                action {
                    components.add(FunctionLineModel("current_weather",
                        "Get the weather for a location",
                        """{"type":"object","properties":{"location":{"type":"string","description":"city and state (e.g. Seattle, WA)"}}}"""
                    ))
                }
            }
            button("Clear all", FontAwesomeIconView(FontAwesomeIcon.TRASH)) {
                action { components.clear() }
            }
        }
    }

    fun functions() = components.mapNotNull {
        try {
            val params = it.parameters.ifNotBlank { Parameters.fromJsonString(it) }
                ?: Parameters.Empty
            ChatCompletionFunction(it.name, it.description, params)
        } catch (x: SerializationException) {
            println(x)
            null
        }
    }
}


class FunctionLineModel(name: String, description: String = "", params: String = "") {
    val nameProperty = SimpleStringProperty(name)
    var name: String by nameProperty

    val descriptionProperty = SimpleStringProperty(description)
    var description: String by descriptionProperty

    // example: {"type":"object","properties":{"location":{"type":"string","description":"city and state"}}}
    val parametersProperty = SimpleStringProperty(params)
    var parameters: String by parametersProperty
}
