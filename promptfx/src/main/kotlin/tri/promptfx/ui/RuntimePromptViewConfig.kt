package tri.promptfx.ui

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
