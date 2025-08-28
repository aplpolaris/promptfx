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
package tri.promptfx.prompts

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptGroupIO
import tri.ai.prompt.PromptLibrary
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.promptFxFileChooser
import tri.promptfx.ui.prompt.PromptDetailsUi
import tri.util.ui.NavigableWorkspaceViewImpl
import java.io.File

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
                        promptIdFilter = { text in it }
                        refilter()
                    }
                }
                spacer()
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
                button("", FontAwesomeIconView(FontAwesomeIcon.FILTER)) {
                    tooltip("Configure prompt library filters.")
                    action {
                        configureFilters()
                    }
                }
            }
            listview(filteredPromptEntries) {
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
                promptSelection.onChange { prompt.set(it) }
                this@output.add(this)
            }
        }
        hideParameters()
        hideRunButton()
    }

    override fun onCreate() {
        TODO()
    }

    private fun sendToTemplateView() {
        find<PromptFxWorkspace>().launchTemplateView(promptSelection.value!!.template!!)
    }
    
    private fun configureFilters() {
        val config = find<PromptFxConfig>()
        val currentFile = config.promptLibraryConfigFile()
        
        confirmation(
            "Configure Prompt Library Filters",
            "Would you like to select a configuration file to filter which prompts are loaded?",
            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL
        ) { result ->
            when (result) {
                ButtonType.YES -> {
                    promptFxFileChooser(
                        "Select Prompt Library Configuration File",
                        arrayOf(PromptFxConfig.FF_YAML, PromptFxConfig.FF_ALL),
                        dirKey = PromptFxConfig.DIR_KEY_DEFAULT
                    ) { files ->
                        if (files.isNotEmpty()) {
                            config.setPromptLibraryConfigFile(files.first())
                            information(
                                "Configuration Updated", 
                                "Prompt library configuration has been updated. Please restart the application for changes to take effect."
                            )
                        }
                    }
                }
                ButtonType.NO -> {
                    if (currentFile != null) {
                        config.setPromptLibraryConfigFile(null)
                        information(
                            "Configuration Cleared", 
                            "Prompt library configuration has been cleared. All prompts will be loaded. Please restart the application for changes to take effect."
                        )
                    } else {
                        information("No Configuration", "No configuration file was previously selected.")
                    }
                }
                // CANCEL - do nothing
            }
        }
    }

    private fun refilter() {
        filteredPromptEntries.setAll(promptEntries.filter { promptIdFilter(it.id) })
    }

    override suspend fun processUserInput() = AiPipelineResult.todo()
}
