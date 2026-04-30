/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.prompt.PromptTemplate

/**
 * General-purpose task input information for execution traces.
 * Replaces [PromptInfo] in new [AiTaskTrace]-based traces, supporting both prompt-template
 * style inputs and arbitrary named-input maps for non-prompt tasks.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiTaskInputInfo(
    /** Prompt template text (if applicable). */
    val prompt: String? = null,
    /** Parameters for filling the prompt template. */
    val params: Map<String, Any> = mapOf(),
    /** Additional named task inputs (e.g., context, history, documents). */
    val inputs: Map<String, Any> = mapOf()
) {
    /** Returns the prompt filled with [params], or null if no prompt is defined. */
    fun filled(): String? = prompt?.let { PromptTemplate(it).fill(params) }

    /** Reconstructs a [PromptInfo] for backward compatibility. */
    fun toPromptInfo(): PromptInfo? = prompt?.let { PromptInfo(it, params) }

    companion object {
        /** Creates an [AiTaskInputInfo] from a [PromptInfo]. */
        fun of(promptInfo: PromptInfo) = AiTaskInputInfo(promptInfo.template, promptInfo.params)

        /** Creates an [AiTaskInputInfo] from a prompt string and optional key-value parameters. */
        fun of(prompt: String, vararg params: Pair<String, Any?>) =
            AiTaskInputInfo(prompt, mapOfNotNull(*params))

        private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
            mapOf(*pairs).filterValues { it != null } as Map<String, Any>
    }
}
