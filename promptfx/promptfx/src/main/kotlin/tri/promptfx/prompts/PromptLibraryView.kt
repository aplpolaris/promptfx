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
package tri.promptfx.prompts

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptLibrary
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.ui.prompt.PromptDetailsUi
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [PromptTemplateView]. */
class PromptLibraryPlugin : NavigableWorkspaceViewImpl<PromptLibraryView>("Prompts", "Prompt Library", type = PromptLibraryView::class)

/** A view designed to help you test prompt templates. */
class PromptLibraryView : AiTaskView("Prompt Library", "View and customize prompt templates.") {

    // TODO - make the prompt details ui stretch the whole height

    private val lib = PromptLibrary.INSTANCE
    private val runtimeLib = PromptLibrary.RUNTIME_INSTANCE

    private val promptEntries = observableListOf(lib.list().toMutableList())
    private var promptIdFilter: (String) -> Boolean = { true }
    private val filteredPromptEntries = observableListOf(promptEntries)
    private val promptSelection = SimpleObjectProperty<PromptDef>()
    private lateinit var promptListView: ListView<PromptDef>

    init {
        input {
            toolbar {
                // add search bar here to update promptFilter when you hit enter
                textfield("") {
                    promptText = "Search"
                    setOnKeyPressed {
                        promptIdFilter = { text in it }
                        refilter()
                    }
                }
                spacer()
                button("", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
                    tooltip("Create a new prompt")
                    action { onCreate() }
                }
                button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                    tooltip("Refresh the prompt list.")
                    action {
                        PromptLibrary.refreshRuntimePrompts()
                        promptEntries.setAll(lib.list().toMutableList())
                        refilter()
                    }
                }
                button("", FontAwesomeIconView(FontAwesomeIcon.EDIT)) {
                    tooltip("Edit custom-prompts.yaml file in your default editor.")
                    action {
                        val file = PromptLibrary.RUNTIME_PROMPTS_FILE
                        if (!file.exists()) {
                            PromptLibrary.createRuntimePromptsFile()
                        }
                        hostServices.showDocument(file.toURI().toString())
                    }
                }
            }
            promptListView = listview(filteredPromptEntries) {
                vgrow = Priority.ALWAYS
                promptSelection.bind(this.selectionModel.selectedItemProperty())
                cellFormat {
                    graphic = Text(it.bareId).apply {
                        tooltip(it.template)
                        if (runtimeLib.get(it.id) != null) {
                            style = "-fx-font-weight: bold"
                            text = it.id + " (customized)"
                        }
                    }
                }
            }
        }
        outputPane.clear()
        output {
            toolbar {
                button("Try in Template View", FontAwesomeIconView(FontAwesomeIcon.SEND)) {
                    enableWhen(promptSelection.isNotNull)
                    action { sendToTemplateView() }
                }
            }
            find<PromptDetailsUi>().apply {
                visibleWhen(promptSelection.isNotNull)
                managedWhen(visibleProperty())
                promptSelection.onChange { prompt.set(it) }
                this@output.add(this)
            }
        }
        hideParameters()
        hideRunButton()
    }

    override fun onCreate() {
        val dialog = find<CreatePromptDialog>()
        dialog.openModal(block = true)
        
        val newPrompt = dialog.getResult()
        if (newPrompt != null) {
            // Refresh the runtime prompts to pick up the new prompt
            PromptLibrary.refreshRuntimePrompts()
            
            // Update the prompt entries list
            promptEntries.setAll(lib.list().toMutableList())
            refilter()
            
            // Select the new prompt
            val createdPrompt = filteredPromptEntries.find { it.id == newPrompt.id }
            if (createdPrompt != null) {
                promptListView.selectionModel.select(createdPrompt)
                promptListView.scrollTo(createdPrompt)
            }
        }
    }

    private fun sendToTemplateView() {
        find<PromptFxWorkspace>().launchTemplateView(promptSelection.value!!.template!!)
    }

    private fun refilter() {
        filteredPromptEntries.setAll(promptEntries.filter { promptIdFilter(it.id) })
    }

    /** Select a prompt in the library that matches the given template text. */
    fun selectPromptByTemplate(templateText: String) {
        // Find a prompt with matching template text
        val matchingPrompt = filteredPromptEntries.find { promptDef ->
            promptDef.template?.trim() == templateText.trim()
        }
        
        if (matchingPrompt != null) {
            promptListView.selectionModel.select(matchingPrompt)
            promptListView.scrollTo(matchingPrompt)
        } else {
            // If no exact match, try to find a prompt that contains the template text
            val partialMatch = filteredPromptEntries.find { promptDef ->
                promptDef.template?.contains(templateText.trim()) == true
            }
            if (partialMatch != null) {
                promptListView.selectionModel.select(partialMatch)
                promptListView.scrollTo(partialMatch)
            }
        }
    }

    override suspend fun processUserInput() = AiPipelineResult.todo()
}
