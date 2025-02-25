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
package tri.promptfx.ui.trace

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TextInputDialog
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.openai.jsonWriter
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TRACE
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.buildsendresultmenu
import tri.promptfx.promptFxFileChooser
import tri.promptfx.tools.PromptTraceFilter
import tri.util.ui.checklistmenu
import tri.util.ui.graphic

/** UI for a list of [AiPromptTrace]s. */
class PromptTraceCardList: Fragment() {

    private val isRemovable: Boolean by param(false)
    private val isShowFilter: Boolean by param(false)
    private val toolbarLabel: String by param("Results:")
    val prompts: ObservableList<AiPromptTraceSupport<*>> by param()

    private val controller: PromptFxController by inject()
    private val isGlobalHistoryView = controller.promptHistory.prompts === prompts
    private val promptFilter: PromptTraceFilter = find<PromptTraceFilter>()
    private val filteredPrompts = observableListOf<AiPromptTraceSupport<*>>()
    private lateinit var promptSelectionModel: MultipleSelectionModel<AiPromptTraceSupport<*>>
    val selectedPrompt = SimpleObjectProperty<AiPromptTraceSupport<*>>()

    init {
        prompts.onChange {
            promptFilter.updateFilterOptions(it.list)
            refilter()
        }
        promptFilter.filter.onChange { refilter() }
        promptFilter.updateFilterOptions(prompts)
        refilter()
    }

    override val root = vbox {
        vgrow = Priority.ALWAYS
        val header = toolbar {
            text(toolbarLabel)
            spacer()
        }
        if (isShowFilter) {
            toolbar {
                label("Filter by:")
                checklistmenu("view", promptFilter.viewFilters) { refilter() }
                checklistmenu("model", promptFilter.modelFilters) { refilter() }
                checklistmenu("status", promptFilter.statusFilters) { refilter() }
                checklistmenu("type", promptFilter.typeFilters) { refilter() }
            }
        }
        val list = listview(filteredPrompts) {
            vgrow = Priority.ALWAYS
            promptSelectionModel = selectionModel
            selectedPrompt.bind(selectionModel.selectedItemProperty())
            cellFormat {
                graphic = PromptTraceCard().apply { setTrace(it) }.root
            }
            // add context menu
            lazyContextmenu {
                item("Details...") {
                    enableWhen(selectionModel.selectedItemProperty().isNotNull)
                    action {
                        val selected = selectionModel.selectedItem
                        if (selected != null)
                            find<PromptTraceDetailsUi>().apply {
                                setTrace(selected)
                                openModal()
                            }
                    }
                }
                item("Try in template view", graphic = FontAwesomeIcon.SEND.graphic) {
                    enableWhen(selectionModel.selectedItemProperty().booleanBinding {
                        it?.prompt?.prompt?.isNotBlank() == true
                    })
                    action {
                        val selected = selectionModel.selectedItem
                        if (selected != null)
                            find<PromptFxWorkspace>().launchTemplateView(selected)
                    }
                }
                if (!isGlobalHistoryView) {
                    item("Open in prompt history view", graphic = FontAwesomeIcon.SEARCH.graphic) {
                        enableWhen(selectionModel.selectedItemProperty().booleanBinding {
                            it?.prompt?.prompt?.isNotBlank() == true
                        })
                        action {
                            val selected = selectionModel.selectedItem
                            if (selected != null)
                                find<PromptFxWorkspace>().launchHistoryView(selected)
                        }
                    }
                }
                buildsendresultmenu("result", selectedPrompt.value?.firstValue?.toString(), find<PromptFxWorkspace>())
            }
        }
        with (header) {
            button("", FontAwesomeIcon.SEND.graphic) {
                tooltip("Try out the selected prompt and inputs in the Prompt Template view.")
                enableWhen(list.selectionModel.selectedItemProperty().isNotNull)
                action {
                    val selected = list.selectedItem
                    if (selected != null)
                        find<PromptFxWorkspace>().launchTemplateView(selected)
                }
            }
            if (isRemovable) {
                button("", FontAwesomeIcon.REMOVE.graphic) {
                    tooltip("Remove selected prompt traces.")
                    enableWhen(list.selectionModel.selectedItemProperty().isNotNull)
                    action {
                        confirm(
                            "Remove traces?",
                            "Are you sure you want to remove selected prompt traces?",
                            owner = currentWindow
                        ) {
                            val selection = list.selectionModel.selectedItems.toList()
                            prompts.removeAll(selection)
                            list.selectionModel.clearSelection()
                        }
                    }
                }
            }
            button("", FontAwesomeIcon.DOWNLOAD.graphic) {
                tooltip("Export prompt traces as JSON.")
                enableWhen(prompts.sizeProperty.greaterThan(0))
                action {
                    exportPromptTraceList()
                }
            }
            if (isRemovable) {
                button("", FontAwesomeIcon.TRASH.graphic) {
                    enableWhen(prompts.sizeProperty.greaterThan(0))
                    action {
                        confirm("Clear all prompt traces?", "Are you sure you want to clear all prompt traces?", owner = currentWindow) {
                            prompts.clear()
                            list.selectionModel.clearSelection()
                        }
                    }
                }
            }

            // settings enabled for global history only
            if (isGlobalHistoryView) {
                button("", FontAwesomeIcon.GEARS.graphic) {
                    tooltip("Adjust prompt history settings.")
                    action {
                        TextInputDialog(controller.promptHistory.maxHistorySize.value.toString()).apply {
                            initOwner(currentWindow)
                            title = "Adjust Prompt History Settings"
                            headerText = "Enter max # of prompt traces to keep in history."
                            contentText = "Max Entries:"
                            showAndWait().ifPresent {
                                it.toIntOrNull()?.let { controller.promptHistory.maxHistorySize.set(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportPromptTraceList() {
        val promptTraces = filteredPrompts.toList()
        promptFxFileChooser(
            dirKey = DIR_KEY_TRACE,
            title = "Export Prompt Traces as JSON",
            filters = arrayOf(FF_JSON, FF_ALL),
            mode = FileChooserMode.Save
        ) { file ->
            file.firstOrNull()?.let {
                runAsync {
                    runBlocking {
                        jsonWriter.writeValue(it, promptTraces)
                    }
                }
            }
        }
    }

    private fun refilter() {
        if (isShowFilter) {
            val filter = promptFilter.filter.value
            filteredPrompts.setAll(prompts.toList().filter(filter))
        } else {
            filteredPrompts.setAll(prompts.toList())
        }
    }

    fun selectPromptTrace(prompt: AiPromptTraceSupport<*>) {
        promptSelectionModel.select(prompt)
    }

}
