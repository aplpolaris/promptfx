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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonValueInstantiator
import tri.util.ui.WorkspaceViewAffordance

/** Configuration for a [RuntimePromptView]. */
class RuntimePromptViewConfig(
    val category: String,
    val title: String,
    val description: String,
    val promptConfig: PromptConfig? = null,
    val promptRef: String? = null,
    val modeOptions: List<ModeConfig> = listOf(),
    val isShowModelParameters: Boolean = false,
    val isShowMultipleResponseOption: Boolean = false,
    val affordances: WorkspaceViewAffordance = WorkspaceViewAffordance.INPUT_ONLY
) {
    init {
        require(promptConfig != null || promptRef != null) { "Either promptConfig or promptRef must be provided." }
        require(promptConfig == null || promptRef == null) { "Only one of promptConfig or promptRef can be provided." }
    }

    fun promptConfig() = promptConfig ?: PromptConfig(id = promptRef!!)

}

/**
 * Reference mode [id] in `modes.yaml` configuration file, associated [templateId] for use in prompt, [label] for UI.
 * Either [values] or [id] must be present.
 */
class ModeConfig(val id: String? = null, val templateId: String, val label: String, val values: List<String>? = null)

/** Prompt config for view. */
class PromptConfig(
    @JsonProperty("id") @JsonAlias("template-name") val id: String,
    val isVisible: Boolean = true,
    @JsonProperty("template-description") val templateDescription: String? = null,
    @JsonProperty("template-prompt") val templatePrompt: String? = null
)
