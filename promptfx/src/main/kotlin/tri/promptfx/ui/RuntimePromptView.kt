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
 * A view with a single input and a single output that can be fully configured at runtime.
 */
open class RuntimePromptView(config: RuntimePromptViewConfig): AiPlanTaskView(
    config.prompt.title(), config.prompt.description!!
) {

    private val modeConfigs = config.modeOptions.map { ModeViewConfig(it) }
    private lateinit var promptModel: PromptSelectionModel
    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        parameters("Prompt") {
            modeConfigs.forEach {
                field(it.label) {
                    combobox(it.mode, it.options) {
                        maxWidth = 200.0
                        isEditable = true
                    }
                }
            }
            if (config.isShowPrompt) {
                promptModel = PromptSelectionModel(config.prompt.id, config.prompt.template)
                promptfield(prompt = promptModel, workspace = workspace)
            }
            if (config.isShowMultipleResponseOption && !config.isShowModelParameters) {
                with(common) {
                    numResponses()
                }
            }
        }
        if (config.isShowModelParameters)
            addDefaultTextCompletionParameters(common)
    }

    override fun plan() = common.completionBuilder()
        .prompt(promptModel.prompt.value)
        .params(modeConfigs.associate { it.templateId to modeTemplateValue(it.id, it.mode.value) })
        .params(singleTemplateFieldName() to input.get())
        .taskPlan(completionEngine)

    /** Returns the name of the first template placeholder variable used for input, or [PromptTemplate.INPUT] as a default. */
    private fun singleTemplateFieldName() =
        promptModel.prompt.value.template().findFields().firstOrNull() ?: PromptTemplate.INPUT

    private fun modeTemplateValue(id: String?, valueOrValueId: String) =
        if (id == null) valueOrValueId else RuntimePromptViewConfigs.modeTemplateValue(id, valueOrValueId)

}

/** Mode config with property indicating current selection. */
internal class ModeViewConfig(config: ModeConfig) {
    val id = config.id
    val templateId = config.templateId
    val label = config.label
    val options: List<String> = config.values ?: RuntimePromptViewConfigs.modeOptionList(id!!)
    val mode = SimpleStringProperty(options[0])
}