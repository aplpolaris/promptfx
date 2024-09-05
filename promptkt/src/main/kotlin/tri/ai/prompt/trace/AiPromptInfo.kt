/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.prompt.AiPrompt.Companion.fill

/** Text prompt generation info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiPromptInfo(
    var prompt: String,
    var promptParams: Map<String, Any> = mapOf()
) {

    /** Fill in the prompt with the parameters. */
    fun filled() = prompt.fill(promptParams)

    companion object {
        const val INSTRUCTION = "instruction"
        const val INPUT = "input"

        /** Create prompt info. */
        fun info(prompt: String, vararg pairs: Pair<String, Any?>) =
            AiPromptInfo(prompt, mapOfNotNull(*pairs))

        private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
            mapOf(*pairs).filterValues { it != null } as Map<String, Any>
    }

}
