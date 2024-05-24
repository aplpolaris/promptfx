/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import tri.ai.core.TextPlugin
import tri.promptfx.AiTaskView
import tri.util.ui.graphic
import java.util.function.Predicate

class ModelsView : AiTaskView("Models", "List all models from API call, sorted by creation date", showInput = false) {

    private val models = observableListOf<ModelInfo>()
    private val sortedModels = models.sorted(compareBy { it.id })
    private val filteredModels = sortedModels.filtered { true }
    private val selectedModel = SimpleObjectProperty<ModelInfo>()

    init {
        hideParameters()
        outputPane.clear()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    init {
        output {
            splitpane {
                vgrow = Priority.ALWAYS
                vbox {
                    vgrow = Priority.ALWAYS
                    toolbar {
                        button("", FontAwesomeIcon.REFRESH.graphic) {
                            action { refresh() }
                        }
                        menubutton("", FontAwesomeIcon.SORT_ALPHA_ASC.graphic) {
                            item("Sort by Id") {
                                action { sortedModels.comparator = compareBy { it.id } }
                            }
                            item("Sort by Created") {
                                action { sortedModels.comparator = compareBy { it.created } }
                            }
                            item("Sort by Source") {
                                action { sortedModels.comparator = compareBy { it.source } }
                            }
                            item("Sort by Type") {
                                action { sortedModels.comparator = compareBy { it.type } }
                            }
                        }
                        menubutton("", FontAwesomeIcon.SORT_ALPHA_DESC.graphic) {
                            item("Sort by Id (descending)") {
                                action { sortedModels.comparator = compareByDescending { it.id } }
                            }
                            item("Sort by Created (descending)") {
                                action { sortedModels.comparator = compareByDescending { it.created } }
                            }
                            item("Sort by Source (descending)") {
                                action { sortedModels.comparator = compareByDescending { it.source } }
                            }
                            item("Sort by Type (descending)") {
                                action { sortedModels.comparator = compareByDescending { it.type } }
                            }
                        }
                        menubutton("", FontAwesomeIcon.FILTER.graphic) {
                            menu("Filter by Source") {
                                TextPlugin.sources().forEach { source ->
                                    item(source) {
                                        action { filteredModels.predicate = Predicate { it.source == source } }
                                    }
                                }
                            }
                            menu("Filter by Type") {
                                ModelType.values().forEach { type ->
                                    item(type.name) {
                                        isMnemonicParsing = false
                                        action { filteredModels.predicate = Predicate { it.type == type } }
                                    }
                                }
                            }
                            item("Clear Filter") {
                                action { filteredModels.predicate = Predicate { true } }
                            }
                        }
                    }

                    listview(filteredModels) {
                        vgrow = Priority.ALWAYS
                        cellFormat {
                            text = "${it.id} (${it.source})"
                        }
                        bindSelected(selectedModel)
                    }
                }
                vbox {
                    vgrow = Priority.ALWAYS

                    scrollpane {
                        vgrow = Priority.ALWAYS

                        form {
                            visibleWhen { selectedModel.isNotNull }
                            managedWhen { selectedModel.isNotNull }

                            fun Fieldset.modelfield(text: String, op: (ModelInfo) -> Any?) {
                                field(text) {
                                    val prop = selectedModel.stringBinding { it?.let { op(it) }?.toString() }
                                    text(prop) {
                                        visibleWhen(prop.isNotNull)
                                        wrappingWidth = 300.0
                                    }
                                }
                            }

                            fieldset("Model Info") {
                                modelfield("Id") { it.id }
                                modelfield("Name") { it.name }
                                modelfield("Type") { it.type }
                                modelfield("Description") { it.description }
                            }
                            fieldset("Model Version") {
                                modelfield("Created") { it.created }
                                modelfield("Source") { it.source }
                                modelfield("Version") { it.version }
                            }
                            fieldset("Model Limits") {
                                modelfield("Input Token Limit") { it.inputTokenLimit }
                                modelfield("Output Token Limit") { it.outputTokenLimit }
                                modelfield("Total Token Limit") { it.totalTokenLimit }
                                modelfield("Output Dimension") { it.outputDimension }
                            }
                            fieldset("Other Properties") {
                                field("Params") {
                                    text(selectedModel.stringBinding {
                                        it?.params?.entries?.joinToString("\n") { (k, v) -> "$k: $v" }
                                    }) {
                                        wrappingWidth = 300.0
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        refresh()
    }

    private fun refresh() {
        runAsync {
            TextPlugin.modelInfo()
        } ui {
            models.setAll(it)
        }
    }

    override suspend fun processUserInput() = TODO()

}
