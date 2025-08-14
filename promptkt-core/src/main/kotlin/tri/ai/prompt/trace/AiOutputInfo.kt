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

/** Text inference output info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiOutputInfo<T>(
    var outputs: List<T>
) {
    /** Convert output using a provided function. */
    fun <S> map(transform: (T) -> S) =
        AiOutputInfo(outputs.map(transform))

    /** Convert list of output to a single output. */
    fun <S> mapList(transform: (List<T>) -> S) =
        AiOutputInfo(listOf(transform(outputs)))

    companion object {
        /** Create output info with a single output. */
        fun <T> output(output: T) = AiOutputInfo(listOf(output))
    }
}
