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
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxModels
import tri.util.ui.checklistmenu
import tri.util.ui.graphic
import tri.util.ui.sortmenu

/** API view showing model information. */
class ModelsView : AiTaskView("Models", "List all models from API call, sorted by creation date", showInput = false) {

    private val models = observableListOf<ModelInfo>()
    private val filter: ModelsFilter = find<ModelsFilter>()
    private val filteredModels = observableListOf<ModelInfo>()
    private val sortedModels = filteredModels.sorted(filter.model.sort.value)
    private val selectedModel = SimpleObjectProperty<ModelInfo>()

    init {
        models.onChange {
            filter.model.updateFilterOptions(it.list)
            refilter()
        }
        filter.model.filter.onChange { refilter() }
        filter.model.sort.onChange { sortedModels.setComparator(it) }
    }

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
                        menubutton("Sort by:", FontAwesomeIcon.SORT.graphic) {
                            sortmenu("Id", filter.model) { it.id }
                            sortmenu("Name", filter.model) { it.name }
                            sortmenu("Created Date", filter.model) { it.created }
                            sortmenu("Source", filter.model) { it.source }
                            sortmenu("Type", filter.model) { it.type }
                            sortmenu("Lifecycle", filter.model) { it.lifecycle }
                            item("Reset", graphic = FontAwesomeIcon.UNDO.graphic) {
                                action { filter.model.resetSort() }
                            }
                        }
                        menubutton("Filter by:", FontAwesomeIcon.FILTER.graphic) {
                            checklistmenu("Source", filter.sourceFilters) { refilter() }
                            checklistmenu("Type", filter.typeFilters, ::graphic) { refilter() }
                            checklistmenu("Lifecycle", filter.lifecycleFilters) { refilter() }
                            checkmenuitem("Show All") {
                                isSelected = true
                                action { filter.model.selectAll() }
                                filter.model.filter.onChange { isSelected = filter.model.isAllSelected() }
                            }
                        }
                    }

                    listview(sortedModels) {
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
            filter.model.resetSort()
            if (it.isEmpty()) {
                error("No models found", "No models were returned, possibly due to a missing API key or failed connection. Check the logs for more information.")
            }
        }
    }

    private fun refilter() {
        val filter = filter.model.filter.value
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