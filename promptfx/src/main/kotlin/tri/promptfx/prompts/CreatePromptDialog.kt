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

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptGroup
import tri.ai.prompt.PromptGroupIO
import tri.ai.prompt.PromptLibrary
import java.io.File

/** Dialog for creating a new prompt definition. */
class CreatePromptDialog : Fragment("Create New Prompt") {

    private val promptId = SimpleStringProperty("")
    private val promptName = SimpleStringProperty("")
    private val promptTitle = SimpleStringProperty("")
    private val promptDescription = SimpleStringProperty("")
    private val promptTemplate = SimpleStringProperty("")
    
    private val validationMessage = SimpleStringProperty("")
    
    private var result: PromptDef? = null

    init {
        // Set up validation
        promptId.addListener { _, _, _ -> validateInput() }
        promptName.addListener { _, _, _ -> validateInput() }
        promptTemplate.addListener { _, _, _ -> validateInput() }
    }

    override val root = borderpane {
        prefWidth = 600.0
        prefHeight = 500.0
        
        center = form {
            paddingAll = 20.0
            
            fieldset("Prompt Information") {
                field("Prompt ID*") {
                    textfield(promptId) {
                        promptText = "e.g., custom/my-prompt@1.0.0"
                        prefColumnCount = 40
                    }
                    label("Full ID including category and version (e.g., custom/my-prompt@1.0.0)") {
                        style {
                            fontSize = 10.px
                            textFill = c("#666666")
                        }
                    }
                }
                field("Name") {
                    textfield(promptName) {
                        promptText = "Human-readable name"
                        prefColumnCount = 40
                    }
                }
                field("Title") {
                    textfield(promptTitle) {
                        promptText = "Display title"
                        prefColumnCount = 40
                    }
                }
                field("Description") {
                    textarea(promptDescription) {
                        promptText = "Brief description of what this prompt does"
                        prefRowCount = 2
                        prefColumnCount = 40
                        isWrapText = true
                    }
                }
                field("Template*") {
                    textarea(promptTemplate) {
                        promptText = "Enter the prompt template using {{variable}} syntax..."
                        prefRowCount = 8
                        prefColumnCount = 40
                        isWrapText = true
                        vgrow = Priority.ALWAYS
                    }
                    label("Use {{variable}} for template variables (e.g., {{input}}, {{instruct}})") {
                        style {
                            fontSize = 10.px
                            textFill = c("#666666")
                        }
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
            button("Create Prompt") {
                enableWhen(validationMessage.isEmpty)
                action {
                    if (confirmAndCreatePrompt()) {
                        close()
                    }
                }
            }
        }
    }

    private fun validateInput() {
        val errors = mutableListOf<String>()
        
        // Check prompt ID
        if (promptId.value.isNullOrBlank()) {
            errors.add("Prompt ID is required")
        } else {
            // Check if prompt ID already exists
            if (PromptLibrary.INSTANCE.get(promptId.value) != null || 
                PromptLibrary.RUNTIME_INSTANCE.get(promptId.value) != null) {
                errors.add("Prompt ID already exists")
            }
            // Basic ID format validation
            if (!promptId.value.contains('/')) {
                errors.add("Prompt ID should include category (e.g., custom/my-prompt@1.0.0)")
            }
        }
        
        // Check template
        if (promptTemplate.value.isNullOrBlank()) {
            errors.add("Template is required")
        }
        
        validationMessage.set(errors.joinToString("; "))
    }

    private fun confirmAndCreatePrompt(): Boolean {
        val confirmDialog = confirmation(
            "Create New Prompt", 
            "Create prompt '${promptId.value}'?\n\nThis will add the prompt to your custom-prompts.yaml file.",
            ButtonType.YES, ButtonType.NO
        )
        
        return if (confirmDialog.result == ButtonType.YES) {
            try {
                createAndSavePrompt()
                information("Success", "Prompt '${promptId.value}' has been created successfully!")
                true
            } catch (e: Exception) {
                error("Error Creating Prompt", "Failed to create prompt: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    private fun createAndSavePrompt() {
        val prompt = PromptDef(
            id = promptId.value,
            name = promptName.value.ifBlank { null },
            title = promptTitle.value.ifBlank { null },
            description = promptDescription.value.ifBlank { null },
            template = promptTemplate.value
        )
        
        // Save to custom-prompts.yaml
        savePromptToCustomFile(prompt)
        
        result = prompt
    }

    private fun savePromptToCustomFile(prompt: PromptDef) {
        val file = PromptLibrary.RUNTIME_PROMPTS_FILE
        
        // Ensure the file exists
        if (!file.exists()) {
            PromptLibrary.createRuntimePromptsFile()
        }
        
        // Read existing content
        val existingGroup = if (file.length() > 0) {
            try {
                PromptGroupIO.readFromFile(file.toPath())
            } catch (e: Exception) {
                // If file is corrupted or empty, create a new group
                PromptGroup("custom", prompts = emptyList())
            }
        } else {
            PromptGroup("custom", prompts = emptyList())
        }
        
        // Add new prompt to existing prompts
        val updatedGroup = existingGroup.copy(
            prompts = existingGroup.prompts + prompt
        )
        
        // Write back to file
        PromptGroupIO.MAPPER.writeValue(file, updatedGroup)
    }

    fun getResult(): PromptDef? = result
}