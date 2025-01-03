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
package tri.promptfx.api

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxModels
import tri.util.ui.checklistmenu
import tri.util.ui.graphic

/** API view showing model information. */
class ModelsView : AiTaskView("Models", "List all models from API call, sorted by creation date", showInput = false) {

    private val models = observableListOf<ModelInfo>()

    private val modelsFilter: ModelsFilter = find<ModelsFilter>()
    private val filteredModels = observableListOf<ModelInfo>()

    private val modelSort = SimpleObjectProperty(ModelInfoSort.ID_ASC).apply {
        onChange { nue -> sortedModels.comparator = nue!!.comparator }
    }
    private val sortedModels = filteredModels.sorted(compareBy { it.id })

    private val selectedModel = SimpleObjectProperty<ModelInfo>()

    init {
        models.onChange {
            modelsFilter.model.updateFilterOptions(it.list)
            refilter()
        }
        modelsFilter.model.filter.onChange { refilter() }
    }

    init {
        hideParameters()
        outputPane.clear()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    private val sortToggle = ToggleGroup()

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
                            label("Filter by:")
                            checklistmenu("Source", modelsFilter.sourceFilters) { refilter() }
                            checklistmenu("Type", modelsFilter.typeFilters, ::graphic) { refilter() }
                            checklistmenu("Lifecycle", modelsFilter.lifecycleFilters) { refilter() }
                            checkmenuitem("Show All") {
                                isSelected = true
                                action { modelsFilter.model.selectAll() }
                                modelsFilter.model.filter.onChange { isSelected = modelsFilter.model.isAllSelected() }
                            }
                        }
                    }

                    listview(filteredModels) {
                        vgrow = Priority.ALWAYS
                        cellFormat {
                            text = "${it.id} (${it.source})"
                            graphic = graphic(it.type)
                            val inPolicy = it.id in PromptFxModels.modelIds()
                            style = when {
                                inPolicy -> ""
                                else -> "-fx-font-style: italic"
                            }
                        }
                        bindSelected(selectedModel)
                    }
                }

                add(ModelInfoCard(selectedModel).root)
            }
        }
    }

    init {
        refresh()
    }

    //region FILTER HELPERS

    private fun refresh() {
        runAsync {
            PromptFxModels.policy.modelInfo()
        } ui {
            models.setAll(it)
            modelSort.value = ModelInfoSort.ID_ASC
            if (it.isEmpty()) {
                error("No models found", "No models were returned, possibly due to a missing API key or failed connection. Check the logs for more information.")
            }
        }
    }

    private fun refilter() {
        val filter = modelsFilter.model.filter.value
        filteredModels.setAll(models.toList().filter(filter))
    }

    //endregion

    override suspend fun processUserInput() = TODO()

}

fun graphic(type: ModelType): Node? = when (type) {
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