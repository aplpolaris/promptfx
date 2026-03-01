/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import tri.ai.prompt.PromptDef
import tri.promptfx.PromptFxGlobals
import tri.util.ui.WorkspaceViewAffordance

/** Configuration for a [RuntimePromptView]. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class RuntimePromptViewConfig(
    @JsonProperty("prompt")
    val promptDef: PromptDef,
    val args: List<RuntimeArgConfig> = listOf(),
    val userControls: RuntimeUserControls = RuntimeUserControls(),
    val requestJson: Boolean = false,
    @get:JsonIgnore
    val affordances: WorkspaceViewAffordance = WorkspaceViewAffordance.INPUT_ONLY
) {

    @get:JsonIgnore
    val prompt by lazy {
        val global = PromptFxGlobals.promptLibrary.get(promptDef.id)
        global?.copyWithOverrides(promptDef) ?: promptDef
    }

}

/**
 * Configure a template argument.
 *  - [fieldId] references the field in the prompt template
 *  - [modeId] references a mode in `modes.yaml` to populate options
 *  - [label] is the display label in the UI
 *  - [values] is an optional list of values to populate options
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class RuntimeArgConfig(
    val fieldId: String,
    val control: RuntimeArgDisplayType = RuntimeArgDisplayType.COMBO_BOX,
    val modeId: String? = null,
    val label: String,
    val values: List<String>? = null
)

/** Determines how the input for a template argument is displayed in the UI. */
enum class RuntimeArgDisplayType {
    COMBO_BOX,
    TEXT_AREA,
    HIDDEN
}

/**
 * Options for displaying user controls in the UI.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class RuntimeUserControls(
    val prompt: Boolean = true,
    val modelParameters: Boolean = false,
    val multipleResponses: Boolean = false
)
