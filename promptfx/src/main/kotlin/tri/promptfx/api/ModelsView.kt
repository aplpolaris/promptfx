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
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import tri.ai.core.TextPlugin
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxModels
import tri.util.ui.graphic
import java.util.function.Predicate

class ModelsView : AiTaskView("Models", "List all models from API call, sorted by creation date", showInput = false) {

    private val models = observableListOf<ModelInfo>()
    private val sortedModels = models.sorted(compareBy { it.id })
    private val filteredModels = sortedModels.filtered { true }
    private val selectedModel = SimpleObjectProperty<ModelInfo>()

    private val modelFilter = SimpleObjectProperty(ModelInfoFilter.ALL).apply {
        onChange { nue -> filteredModels.predicate = Predicate { nue!!.filter.invoke(it) } }
    }
    private val modelSort = SimpleObjectProperty(ModelInfoSort.ID_ASC).apply {
        onChange { nue -> sortedModels.comparator = nue!!.comparator }
    }

    init {
        hideParameters()
        outputPane.clear()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    private val sortToggle = ToggleGroup()
    private val filterToggle = ToggleGroup()

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
                            radiomenuitem("Sort by Id", sortToggle) {
                                isSelected = true
                                action { modelSort.value = ModelInfoSort.ID_ASC }
                                // listen to changes in modelSort because this can be selected by reset button
                                modelSort.onChange {
                                    if (it == ModelInfoSort.ID_ASC)
                                        isSelected = true
                                }
                            }
                            radiomenuitem("Sort by Created", sortToggle) {
                                action { modelSort.value = ModelInfoSort.CREATED_ASC }
                            }
                            radiomenuitem("Sort by Source", sortToggle) {
                                action { modelSort.value = ModelInfoSort.SOURCE_ASC }
                            }
                            radiomenuitem("Sort by Type", sortToggle) {
                                action { modelSort.value = ModelInfoSort.TYPE_ASC }
                            }
                        }
                        menubutton("", FontAwesomeIcon.SORT_ALPHA_DESC.graphic) {
                            radiomenuitem("Sort by Id (descending)", sortToggle) {
                                action { modelSort.value = ModelInfoSort.ID_DESC }
                            }
                            radiomenuitem("Sort by Created (descending)", sortToggle) {
                                action { modelSort.value = ModelInfoSort.CREATED_DESC }
                            }
                            radiomenuitem("Sort by Source (descending)", sortToggle) {
                                action { modelSort.value = ModelInfoSort.SOURCE_DESC }
                            }
                            radiomenuitem("Sort by Type (descending)", sortToggle) {
                                action { modelSort.value = ModelInfoSort.TYPE_DESC }
                            }
                        }
                        menubutton("", FontAwesomeIcon.FILTER.graphic) {
                            menu("Filter by Source") {
                                TextPlugin.sources().forEach { source ->
                                    radiomenuitem(source, filterToggle) {
                                        if (source == "Gemini")
                                            graphic = FontAwesomeIcon.GOOGLE.graphic
                                        action { modelFilter.value = ModelInfoFilter.ofSource(source) }
                                    }
                                }
                            }
                            menu("Filter by Type") {
                                ModelType.values().forEach { type ->
                                    radiomenuitem(type.name, filterToggle) {
                                        graphic = type.graphic()
                                        isMnemonicParsing = false
                                        action { modelFilter.value = ModelInfoFilter.ofType(type) }
                                    }
                                }
                            }
                            radiomenuitem("No Filter", filterToggle) {
                                isSelected = true
                                action { modelFilter.value = ModelInfoFilter.ALL }
                                // listen to changes in modelFilter because this can be selected by reset button
                                modelFilter.onChange {
                                    if (it == ModelInfoFilter.ALL)
                                        isSelected = true
                                }
                            }
                        }
                    }

                    listview(filteredModels) {
                        vgrow = Priority.ALWAYS
                        cellFormat {
                            text = "${it.id} (${it.source})"
                            graphic = it.type.graphic()
                            val inPolicy = it.id in PromptFxModels.modelIds()
                            style = when {
                                inPolicy -> ""
                                else -> "-fx-font-style: italic"
                            }
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
            modelSort.value = ModelInfoSort.ID_ASC
            modelFilter.value = ModelInfoFilter.ALL
            if (it.isEmpty()) {
                error("No models found", "No models were returned, possibly due to a missing API key. Check the logs for more information.")
            }
        }
    }

    private fun ModelType.graphic() = when (this) {
        ModelType.TEXT_COMPLETION -> FontAwesomeIcon.KEYBOARD_ALT.graphic
        ModelType.TEXT_CHAT -> FontAwesomeIcon.COMMENTS.graphic
        ModelType.TEXT_VISION_CHAT -> FontAwesomeIcon.IMAGE.graphic
        ModelType.TEXT_EMBEDDING -> FontAwesomeIcon.FONT.graphic
        ModelType.IMAGE_GENERATOR -> FontAwesomeIcon.CAMERA.graphic
        ModelType.TEXT_TO_SPEECH -> FontAwesomeIcon.VOLUME_UP.graphic
        ModelType.SPEECH_TO_TEXT -> FontAwesomeIcon.MICROPHONE.graphic
        ModelType.MODERATION -> FontAwesomeIcon.EYE_SLASH.graphic
        ModelType.QUESTION_ANSWER -> FontAwesomeIcon.QUESTION_CIRCLE.graphic
        ModelType.UNKNOWN -> FontAwesomeIcon.CUBE.graphic
    }

    override suspend fun processUserInput() = TODO()

}

/** Sort options for models. */
enum class ModelInfoSort(val comparator: Comparator<ModelInfo>) {
    ID_ASC(compareBy { it.id }),
    ID_DESC(compareByDescending { it.id }),
    CREATED_ASC(compareBy { it.created }),
    CREATED_DESC(compareByDescending { it.created }),
    SOURCE_ASC(compareBy { it.source }),
    SOURCE_DESC(compareByDescending { it.source }),
    TYPE_ASC(compareBy { it.type }),
    TYPE_DESC(compareByDescending { it.type });
}

/** Filter options for models. */
enum class ModelInfoFilter(val filter: (ModelInfo) -> Boolean) {
    SOURCE_OPENAI({ it.source == "OpenAI" }),
    SOURCE_GEMINI({ it.source == "Gemini" }),
    TYPE_TEXT_COMPLETION({ it.type == ModelType.TEXT_COMPLETION }),
    TYPE_TEXT_CHAT({ it.type == ModelType.TEXT_CHAT }),
    TYPE_TEXT_VISION_CHAT({ it.type == ModelType.TEXT_VISION_CHAT }),
    TYPE_TEXT_EMBEDDING({ it.type == ModelType.TEXT_EMBEDDING }),
    TYPE_IMAGE_GENERATOR({ it.type == ModelType.IMAGE_GENERATOR }),
    TYPE_TEXT_TO_SPEECH({ it.type == ModelType.TEXT_TO_SPEECH }),
    TYPE_SPEECH_TO_TEXT({ it.type == ModelType.SPEECH_TO_TEXT }),
    TYPE_MODERATION({ it.type == ModelType.MODERATION }),
    TYPE_QUESTION_ANSWER({ it.type == ModelType.QUESTION_ANSWER }),
    TYPE_UNKNOWN({ it.type == ModelType.UNKNOWN }),
    ALL({ true });

    companion object {
        fun ofType(type: ModelType) = valueOf("TYPE_${type.name}")
        fun ofSource(source: String) = valueOf("SOURCE_${source.uppercase()}")
    }
}
