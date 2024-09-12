/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx

import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.openai.templatePlan
import tri.ai.prompt.AiPrompt
import tri.promptfx.ui.promptfield

/**
 * A view with a single input and a single output that can be fully configured at runtime.
 */
open class RuntimePromptView(config: RuntimePromptViewConfig): AiPlanTaskView(config.title, config.description) {

    private val modeConfigs = config.modeOptions.map { ModeViewConfig(it) }
    private val promptConfig = config.promptConfig
    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        parameters("Prompt") {
            modeConfigs.forEach {
                field(it.label) {
                    combobox(it.mode, it.options) { isEditable = true }
                }
            }
            if (promptConfig.isVisible) {
                promptfield(promptId = promptConfig.id, workspace = workspace)
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

    override fun plan() = completionEngine.templatePlan(
        promptId = promptConfig.id,
        fields = modeConfigs.associate { it.idInTemplate to RuntimePromptViewConfigs.modeTemplateValue(it.id, it.mode.get()) }
                + mapOf(AiPrompt.INPUT to input.get()),
        tokenLimit = common.maxTokens.value,
        temp = common.temp.value,
        numResponses = common.numResponses.value
    )

    /** Mode config with property indicating current selection. */
    inner class ModeViewConfig(config: ModeConfig) {
        val id = config.id
        val idInTemplate = config.templateId
        val label = config.label
        val options: List<String> = RuntimePromptViewConfigs.modeOptionList(id)
        val mode = SimpleStringProperty(options[0])
    }

}

/** Configuration for a [RuntimePromptView]. */
class RuntimePromptViewConfig(
    val category: String,
    val title: String,
    val description: String,
    val modeOptions: List<ModeConfig>,
    val promptConfig: PromptConfig,
    val isShowModelParameters: Boolean,
    val isShowMultipleResponseOption: Boolean = false
)

/** Reference mode [id] in configuration file, associated [templateId] for use in prompt, [label] for UI. */
class ModeConfig(val id: String, val templateId: String, val label: String)

/** Prompt config for view. */
class PromptConfig(val id: String, val isVisible: Boolean = true)
