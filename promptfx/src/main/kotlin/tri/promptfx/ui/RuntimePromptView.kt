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

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.taskPlan
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.template
import tri.promptfx.AiPlanTaskView
import tri.promptfx.RuntimePromptViewConfigs

/**
 * A view with a single input and a single output that can be fully configured at runtime.
 */
open class RuntimePromptView(config: RuntimePromptViewConfig): AiPlanTaskView(
    config.prompt.title(), config.prompt.description ?: "(no description found for ${config.prompt.title()})"
) {

    private val argConfigs = config.args.map { ArgViewConfig(config.prompt.args.find { a -> a.name == it.fieldId }, it) }
    private var promptModel = PromptSelectionModel(config.prompt.id, config.prompt.template)
    private val textAreaInputs = mutableMapOf<String, SimpleStringProperty>()
    private val requestJson = SimpleBooleanProperty(false)

    init {
        addInputTextAreas()
        parameters("Prompt") {
            argConfigs.filter { it.config.control == RuntimeArgDisplayType.COMBO_BOX }.forEach {
                field(it.label) {
                    combobox(it.mode, it.options) {
                        maxWidth = 200.0
                        isEditable = true
                    }
                }
            }
            if (config.userControls.prompt) {
                promptfield(prompt = promptModel, workspace = workspace)
            }
            if (config.userControls.multipleResponses && !config.userControls.modelParameters) {
                with(common) {
                    numResponses()
                }
            }
        }
        if (config.userControls.modelParameters)
            addDefaultChatParameters(common)
        if (config.userControls.modelParameters && config.requestJson) {
            parameters("Response") {
                field("Response Format") {
                    tooltip("Important: when using JSON mode, you must also instruct the model to produce JSON yourself via a system or user message.")
                    checkbox("JSON (if supported)", requestJson)
                }
            }
        }
    }

    /** Add text areas for any args configured as such, otherwise a single text area for the first template field. */
    private fun addInputTextAreas() {
        val textAreaArgs = argConfigs.filter { it.config.control == RuntimeArgDisplayType.TEXT_AREA }
        if (textAreaArgs.isNotEmpty()) {
            textAreaArgs.forEach { argConfig ->
                input {
                    val property = SimpleStringProperty(argConfig.defaultValue)
                    textAreaInputs[argConfig.fieldId] = property
                    toolbar {
                        text("${argConfig.label}:")
                    }
                    textarea(property) {
                        vgrow = Priority.ALWAYS
                        isWrapText = true
                        argConfig.description?.let {
                            promptText = it
                            tooltip(it)
                        }
                    }
                }
            }
        } else {
            // Fallback to single input if no text area args specified (backward compatibility)
            textAreaInputs[singleTemplateFieldName()] = SimpleStringProperty().also {
                addInputTextArea(it)
            }
        }
    }

    override fun plan() = common.completionBuilder()
        .prompt(promptModel.prompt.value)
        .params(argConfigs.filter { it.config.control == RuntimeArgDisplayType.COMBO_BOX }
            .associate { it.config.fieldId to modeTemplateValue(it.config.modeId, it.mode.value) })
        .params(textAreaInputs.mapValues { it.value.value ?: "" }.filter { it.value.isNotBlank() })
        .requestJson(if (requestJson.value) true else null)
        .taskPlan(chatEngine)

    /** Returns the name of the first template placeholder variable used for input, or [PromptTemplate.INPUT] as a default. */
    private fun singleTemplateFieldName() =
        promptModel.prompt.value.template().findFields().firstOrNull() ?: PromptTemplate.INPUT

    private fun modeTemplateValue(id: String?, valueOrValueId: String) =
        if (id == null) valueOrValueId else RuntimePromptViewConfigs.modeTemplateValue(id, valueOrValueId)

}

/** Mode config with property indicating current selection. */
internal class ArgViewConfig(
    val argDef: PromptArgDef?,
    val config: RuntimeArgConfig
) {
    val fieldId
        get() = config.fieldId
    val label
        get() = config.label
    val description
        get() = argDef?.description
    val defaultValue
        get() = config.values?.firstOrNull() ?: argDef?.defaultValue

    val options = config.values
        ?: config.modeId?.let { RuntimePromptViewConfigs.modeOptionList(it) }
        ?: argDef?.allowedValues
        ?: listOf("")
    val mode = SimpleStringProperty(options.getOrNull(0) ?: "")
}