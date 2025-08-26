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
package tri.promptfx.settings

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.prompt.PromptDef
import tri.promptfx.PromptFxWorkspaceModel
import tri.promptfx.RuntimePromptViewConfigs
import tri.promptfx.ui.ModeConfig
import tri.promptfx.ui.RuntimePromptViewConfig
import tri.util.ui.WorkspaceViewAffordance

/** Dialog for creating a new custom view configuration. */
class NewViewDialog : Fragment("Create New Custom View") {

    private val viewName = SimpleStringProperty("")
    private val viewCategory = SimpleStringProperty("")
    private val viewDescription = SimpleStringProperty("")
    
    private val useExistingPrompt = SimpleBooleanProperty(true)
    private val selectedPromptId = SimpleStringProperty("")
    private val customTemplate = SimpleStringProperty("")
    
    private val showModelParameters = SimpleBooleanProperty(false)
    private val showMultipleResponses = SimpleBooleanProperty(false)
    
    private val validationMessage = SimpleStringProperty("")
    
    private var result: RuntimePromptViewConfig? = null

    init {
        // Set up validation
        viewName.addListener { _, _, _ -> validateInput() }
        selectedPromptId.addListener { _, _, _ -> validateInput() }
        customTemplate.addListener { _, _, _ -> validateInput() }
        useExistingPrompt.addListener { _, _, _ -> validateInput() }
    }

    override val root = borderpane {
        prefWidth = 600.0
        prefHeight = 500.0
        
        center = form {
            paddingAll = 20.0
            
            fieldset("View Information") {
                field("View Name*") {
                    textfield(viewName) {
                        promptText = "Enter a unique name for the view"
                        prefColumnCount = 30
                    }
                }
                field("Category*") {
                    textfield(viewCategory) {
                        promptText = "e.g., Text, Math, Code"
                        prefColumnCount = 30
                    }
                }
                field("Description") {
                    textarea(viewDescription) {
                        promptText = "Optional description of what this view does"
                        prefRowCount = 2
                        prefColumnCount = 30
                        isWrapText = true
                    }
                }
            }
            
            fieldset("Prompt Configuration") {
                field("Prompt Source") {
                    val toggleGroup = ToggleGroup()
                    vbox(5) {
                        radiobutton("Use Existing Prompt", toggleGroup, value = useExistingPrompt) {
                            isSelected = true
                        }
                        hbox(5) {
                            alignment = Pos.CENTER_LEFT
                            label("Prompt ID:")
                            combobox<String>(selectedPromptId) {
                                items = getAvailablePromptIds().asObservable()
                                isEditable = true
                                promptText = "Select or enter prompt ID"
                                prefWidth = 300.0
                                enableWhen(useExistingPrompt)
                            }
                        }
                        
                        radiobutton("Create New Prompt", toggleGroup, value = useExistingPrompt.not()) 
                        
                        textarea(customTemplate) {
                            promptText = "Enter your prompt template here..."
                            prefRowCount = 5
                            prefColumnCount = 40
                            isWrapText = true
                            enableWhen(useExistingPrompt.not())
                        }
                    }
                }
            }
            
            fieldset("View Options") {
                field("Display Options") {
                    vbox(5) {
                        checkbox("Show Model Parameters", showModelParameters)
                        checkbox("Show Multiple Response Option", showMultipleResponses)
                    }
                }
            }
            
            // Validation message area
            label(validationMessage) {
                style {
                    textFill = c("#cc0000")
                    fontSize = 12.px
                }
                visibleWhen(validationMessage.isNotEmpty)
            }
        }
        
        bottom = hbox(10) {
            paddingAll = 10.0
            alignment = Pos.CENTER_RIGHT
            
            button("Cancel") {
                action { close() }
            }
            button("Create View") {
                enableWhen(validationMessage.isEmpty)
                action {
                    if (confirmAndCreateView()) {
                        close()
                    }
                }
            }
        }
    }

    private fun getAvailablePromptIds(): List<String> {
        return RuntimePromptViewConfigs.PROMPT_LIBRARY.list().map { it.id }.sorted()
    }

    private fun validateInput() {
        val errors = mutableListOf<String>()
        
        // Check view name
        if (viewName.value.isNullOrBlank()) {
            errors.add("View name is required")
        } else {
            // Check for duplicate names by generating the view ID and checking if it exists
            val potentialViewId = if (viewCategory.value.isNotBlank()) {
                ViewConfigManager.generateViewId(viewCategory.value, viewName.value)
            } else {
                viewName.value.lowercase().replace(" ", "-")
            }
            
            // Only check for duplicates if we're not generating a unique ID
            val baseId = if (viewCategory.value.isNotBlank()) {
                "${viewCategory.value.lowercase().replace(" ", "-")}-${viewName.value.lowercase().replace(" ", "-")}"
            } else {
                viewName.value.lowercase().replace(" ", "-")
            }
            
            if (ViewConfigManager.viewExists(baseId)) {
                errors.add("A view with this name already exists in this category")
            }
        }
        
        // Check category
        if (viewCategory.value.isNullOrBlank()) {
            errors.add("Category is required")
        }
        
        // Check prompt configuration
        if (useExistingPrompt.value) {
            if (selectedPromptId.value.isNullOrBlank()) {
                errors.add("Prompt ID is required when using existing prompt")
            }
        } else {
            if (customTemplate.value.isNullOrBlank()) {
                errors.add("Template is required when creating new prompt")
            }
        }
        
        validationMessage.set(errors.joinToString("; "))
    }

    private fun confirmAndCreateView(): Boolean {
        val confirmDialog = confirmation(
            "Create New View", 
            "Create '${viewName.value}' in category '${viewCategory.value}'?\n\nThis will add the view to your config/views.yaml file and make it available immediately.",
            ButtonType.YES, ButtonType.NO
        )
        
        return if (confirmDialog.result == ButtonType.YES) {
            try {
                createAndSaveViewConfig()
                information("Success", "View '${viewName.value}' has been created successfully!\n\nNote: You may need to restart PromptFx to see the new view in the navigation menu.")
                true
            } catch (e: Exception) {
                error("Error Creating View", "Failed to create view: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    private fun createAndSaveViewConfig() {
        val promptDef = if (useExistingPrompt.value) {
            PromptDef(
                id = selectedPromptId.value,
                category = viewCategory.value,
                name = viewName.value,
                description = viewDescription.value.ifBlank { null }
            )
        } else {
            PromptDef(
                id = "${viewCategory.value.lowercase().replace(" ", "-")}/${viewName.value.lowercase().replace(" ", "-")}",
                category = viewCategory.value,
                name = viewName.value,
                description = viewDescription.value.ifBlank { null },
                template = customTemplate.value
            )
        }
        
        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            modeOptions = listOf(), // For now, keep it simple
            isShowModelParameters = showModelParameters.value,
            isShowMultipleResponseOption = showMultipleResponses.value,
            affordances = WorkspaceViewAffordance.INPUT_ONLY
        )
        
        // Generate unique view ID and save
        val viewId = ViewConfigManager.generateViewId(viewCategory.value, viewName.value)
        ViewConfigManager.addView(viewId, config)
        
        // Reload runtime configurations to make the new view available
        RuntimePromptViewConfigs.reload()
        
        // Refresh workspace model to update navigation
        PromptFxWorkspaceModel.reload()
        
        result = config
    }

    fun getResult(): RuntimePromptViewConfig? = result
}