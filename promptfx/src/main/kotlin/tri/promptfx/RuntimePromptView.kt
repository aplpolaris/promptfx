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
    )

    /** Mode config with property indicating current selection. */
    inner class ModeViewConfig(config: ModeConfig) {
        val id = config.id
        val idInTemplate = config.templateId
        val label = config.label
        val options: List<String> = RuntimePromptViewConfigs.modes[id]!!.keys.toList()
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
    val isShowModelParameters: Boolean
)

/** Reference mode [id] in configuration file, associated [templateId] for use in prompt, [label] for UI. */
class ModeConfig(val id: String, val templateId: String, val label: String)

/** Prompt config for view. */
class PromptConfig(val id: String, val isVisible: Boolean = true)