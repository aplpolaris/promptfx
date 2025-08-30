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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import tri.ai.prompt.PromptDef
import tri.promptfx.PromptFxGlobals
import tri.util.ui.WorkspaceViewAffordance

/** Configuration for a [RuntimePromptView]. */
class RuntimePromptViewConfig(
    @JsonProperty("prompt")
    val promptDef: PromptDef,
    val argOptions: List<ArgConfig> = listOf(),
    // Backward compatibility - deprecated in favor of argOptions
    @Deprecated("Use argOptions instead") 
    val modeOptions: List<ArgConfig> = listOf(),
    val isShowPrompt: Boolean = true,
    val isShowModelParameters: Boolean = false,
    val isShowMultipleResponseOption: Boolean = false,
    val requestJson: Boolean = false,
    @get:JsonIgnore
    val affordances: WorkspaceViewAffordance = WorkspaceViewAffordance.INPUT_ONLY
) {

    /** Get all argument configurations, combining argOptions and legacy modeOptions. */
    @get:JsonIgnore
    val allArgOptions: List<ArgConfig> get() = argOptions + modeOptions

    @get:JsonIgnore
    val prompt by lazy {
        val global = PromptFxGlobals.promptLibrary.get(promptDef.id)
        global?.copyWithOverrides(promptDef) ?: promptDef
    }

}

/**
 * Configuration for arguments in a prompt view, supporting different display types.
 * Either [values] or [id] must be present for combo box type.
 */
class ArgConfig(
    val id: String? = null,
    val templateId: String,
    val label: String,
    val values: List<String>? = null,
    val description: String? = null,
    @JsonProperty("displayType") 
    val displayType: ArgDisplayType = ArgDisplayType.COMBO_BOX,
    val defaultValue: String? = null
)

/** How an argument should be displayed in the UI. */
enum class ArgDisplayType {
    /** Show as an input text area on the left side */
    TEXT_AREA,
    /** Show as a combo box in the parameters panel */
    COMBO_BOX,
    /** Use default value and hide from UI */
    HIDDEN
}