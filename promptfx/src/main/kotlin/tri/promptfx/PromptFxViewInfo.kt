package tri.promptfx

import tornadofx.*
import tri.util.ui.WorkspaceViewAffordance

/** Information about a view in PromptFx. */
data class PromptFxViewInfo(
    val group: String,
    val name: String,
    val view: Class<out UIComponent>,
    val affordances: WorkspaceViewAffordance = WorkspaceViewAffordance.NONE
)