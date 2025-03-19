package tri.promptfx.ui

import tri.util.ui.WorkspaceViewAffordance

/** Configuration for a [RuntimePromptView]. */
class RuntimePromptViewConfig(
    val category: String,
    val title: String,
    val description: String,
    val promptConfig: PromptConfig,
    val modeOptions: List<ModeConfig> = listOf(),
    val isShowModelParameters: Boolean = false,
    val isShowMultipleResponseOption: Boolean = false,
    val affordances: WorkspaceViewAffordance = WorkspaceViewAffordance.INPUT_ONLY
)

/**
 * Reference mode [id] in `modes.yaml` configuration file, associated [templateId] for use in prompt, [label] for UI.
 * Either [values] or [id] must be present.
 */
class ModeConfig(val id: String? = null, val templateId: String, val label: String, val values: List<String>? = null)

/** Prompt config for view. */
class PromptConfig(val id: String, val isVisible: Boolean = true)
