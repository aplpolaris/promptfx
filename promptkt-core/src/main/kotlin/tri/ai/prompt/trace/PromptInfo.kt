/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.prompt.PromptTemplate

/**
 * A prompt template and parameters used to fill values, for execution traces.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class PromptInfo(
    var template: String,
    var params: Map<String, Any> = mapOf()
) {

    /** Create prompt info with a template and parameters. */
    constructor(prompt: String, vararg pairs: Pair<String, Any?>) :
            this(prompt, mapOfNotNull(*pairs))

    companion object {
        /** Fill in the prompt with the parameters. */
        fun PromptInfo.filled() =
            PromptTemplate(template).fill(params)

        private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
            mapOf(*pairs).filterValues { it != null } as Map<String, Any>
    }

}
