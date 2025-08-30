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
package tri.promptfx.ui

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.taskPlan
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.template
import tri.promptfx.AiPlanTaskView
import tri.promptfx.RuntimePromptViewConfigs

/**
 * A view with flexible input/output configuration that can be fully configured at runtime.
 */
open class RuntimePromptView(private val viewConfig: RuntimePromptViewConfig): AiPlanTaskView(
    viewConfig.prompt.title(), viewConfig.prompt.description!!
) {

    private val argConfigs = viewConfig.allArgOptions.map { ArgViewConfig(it) }
    private lateinit var promptModel: PromptSelectionModel
    private val textAreaInputs = mutableMapOf<String, SimpleStringProperty>()

    init {
        // Add text area inputs first (on the left side)
        val textAreaArgs = argConfigs.filter { it.displayType == ArgDisplayType.TEXT_AREA }
        if (textAreaArgs.isNotEmpty()) {
            textAreaArgs.forEach { argConfig ->
                val property = SimpleStringProperty(argConfig.defaultValue ?: "")
                textAreaInputs[argConfig.templateId] = property
                
                input {
                    toolbar {
                        text("${argConfig.label}:")
                    }
                    textarea(property) {
                        isWrapText = true
                        if (argConfig.description != null) {
                            promptText = argConfig.description
                            tooltip(argConfig.description)
                        }
                    }
                }
            }
        } else {
            // Fallback to single input if no text area args specified (backward compatibility)
            val defaultInput = SimpleStringProperty("")
            textAreaInputs[singleTemplateFieldName(viewConfig)] = defaultInput
            addInputTextArea(defaultInput)
        }

        parameters("Prompt") {
            // Add combo box parameters
            argConfigs.filter { it.displayType == ArgDisplayType.COMBO_BOX }.forEach { argConfig ->
                field(argConfig.label) {
                    combobox(argConfig.mode, argConfig.options) {
                        maxWidth = 200.0
                        isEditable = true
                        if (argConfig.description != null) {
                            tooltip(argConfig.description)
                        }
                    }
                }
            }
            
            if (viewConfig.isShowPrompt) {
                promptModel = PromptSelectionModel(viewConfig.prompt.id, viewConfig.prompt.template)
                promptfield(prompt = promptModel, workspace = workspace)
            }
            if (viewConfig.isShowMultipleResponseOption && !viewConfig.isShowModelParameters) {
                with(common) {
                    numResponses()
                }
            }
        }
        if (viewConfig.isShowModelParameters)
            addDefaultChatParameters(common)
            
        // Add response format section if JSON is requested
        if (viewConfig.requestJson) {
            parameters("Response") {
                field("Response Format") {
                    tooltip("Important: when using JSON mode, you must also instruct the model to produce JSON yourself via a system or user message.")
                    label("JSON (configured)")
                }
            }
        }
    }

    override fun plan() = common.completionBuilder()
        .prompt(promptModel.prompt.value)
        .params(buildParameterMap())
        .requestJson(if (viewConfig.requestJson) true else null)
        .taskPlan(chatEngine)

    private fun buildParameterMap(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        // Add text area inputs
        textAreaInputs.forEach { (templateId, property) ->
            params[templateId] = property.get()
        }
        
        // Add combo box selections and hidden values
        argConfigs.forEach { argConfig ->
            when (argConfig.displayType) {
                ArgDisplayType.COMBO_BOX -> {
                    params[argConfig.templateId] = modeTemplateValue(argConfig.id, argConfig.mode.value)
                }
                ArgDisplayType.HIDDEN -> {
                    if (argConfig.defaultValue != null) {
                        params[argConfig.templateId] = argConfig.defaultValue
                    }
                }
                ArgDisplayType.TEXT_AREA -> {
                    // Already handled above
                }
            }
        }
        
        return params
    }

    /** Returns the name of the first template placeholder variable used for input, or [PromptTemplate.INPUT] as a default. */
    private fun singleTemplateFieldName(config: RuntimePromptViewConfig) =
        config.prompt.template().findFields().firstOrNull() ?: PromptTemplate.INPUT

    private fun modeTemplateValue(id: String?, valueOrValueId: String) =
        if (id == null) valueOrValueId else RuntimePromptViewConfigs.modeTemplateValue(id, valueOrValueId)

}

/** Argument config with properties for UI binding. */
internal class ArgViewConfig(config: ArgConfig) {
    val id = config.id
    val templateId = config.templateId
    val label = config.label
    val description = config.description
    val displayType = config.displayType
    val defaultValue = config.defaultValue
    val options: List<String> = when (config.displayType) {
        ArgDisplayType.COMBO_BOX -> config.values ?: RuntimePromptViewConfigs.modeOptionList(id!!)
        else -> emptyList()
    }
    val mode = SimpleStringProperty(when (config.displayType) {
        ArgDisplayType.COMBO_BOX -> defaultValue ?: options.firstOrNull() ?: ""
        else -> defaultValue ?: ""
    })
}