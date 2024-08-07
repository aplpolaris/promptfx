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

/** Model configuration info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiPromptModelInfo(
    var modelId: String,
    var modelParams: Map<String, Any> = mapOf()
) {
    companion object {
        const val MAX_TOKENS = "max_tokens"
        const val STOP = "stop"
        const val TEMPERATURE = "temperature"
        const val TOP_P = "top_p"
        const val FREQUENCY_PENALTY = "frequency_penalty"
        const val PRESENCE_PENALTY = "presence_penalty"
    }
}
