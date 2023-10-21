/*-
 * #%L
 * promptfx-0.1.8
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
package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxWorkspace
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [PromptTemplateView]. */
class PromptLibraryPlugin : NavigableWorkspaceViewImpl<PromptLibraryView>("Tools", "Prompt Library", PromptLibraryView::class)

/** A view designed to help you test prompt templates. */
class PromptLibraryView : AiTaskView("Prompt Library", "View and customize prompt templates.") {

    val lib = AiPromptLibrary.INSTANCE
    val customLib = AiPromptLibrary.RUNTIME_INSTANCE

    val promptEntries = observableListOf(lib.prompts.entries.toMutableList())
    var promptFilter: (String) -> Boolean = { true }
    val filteredPromptEntries = observableListOf(promptEntries)
    val promptSelection = SimpleObjectProperty<MutableMap.MutableEntry<String, AiPrompt>>()
    val promptTemplate = Bindings.createStringBinding({ promptSelection.value?.value?.template ?: "" }, promptSelection)

    init {
        input {
            toolbar {
                // TODO - tools for prompt CRUD
//                button("Add", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
//                    action { onCreate() }
//                }
                // add search bar here to update promptFilter when you hit enter
                textfield("") {
                    promptText = "Search"
                    setOnKeyPressed {
                        promptFilter = { text in it }
                        refilter()
                    }
                }

                button("Send to Template View", FontAwesomeIconView(FontAwesomeIcon.SEND)) {
                    enableWhen(promptSelection.isNotNull)
                    action { sendToTemplateView() }
                }
                spacer()
                button("", FontAwesomeIconView(FontAwesomeIcon.EDIT)) {
                    tooltip("Edit the custom prompts.yaml file in your default editor.")
                    action {
                        val file = AiPromptLibrary.RUNTIME_PROMPTS_FILE
                        if (!file.exists()) {
                            file.parentFile.mkdirs()
                            file.createNewFile()
                        }
                        hostServices.showDocument(file.toURI().toString())
                    }
                }
            }
            listview(filteredPromptEntries) {
                vgrow = Priority.ALWAYS
                promptSelection.bind(this.selectionModel.selectedItemProperty())
                cellFormat {
                    graphic = Text(it.key).apply {
                        tooltip(it.value.template)
                        if (it.key in customLib.prompts) {
                            style = "-fx-font-weight: bold"
                            text = it.key + " (customized)"
                        }
                    }
                }
            }
        }
        outputPane.clear()
        output {
            textarea(promptTemplate) {
                isEditable = true
                isWrapText = true
                vgrow = Priority.ALWAYS
            }
        }
    }

    override fun onCreate() {
        TODO()
    }

    private fun sendToTemplateView() {
        (workspace as PromptFxWorkspace).launchTemplateView(promptSelection.value!!.value.template)
    }

    private fun refilter() {
        filteredPromptEntries.setAll(promptEntries.filter { promptFilter(it.key) })
    }

    override suspend fun processUserInput() = AiPipelineResult.todo()
}
