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
import java.util.UUID.randomUUID

/** Details of an executed prompt, including prompt configuration, model configuration, execution metadata, and output. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptTrace(
    promptInfo: AiPromptInfo,
    modelInfo: AiPromptModelInfo,
    execInfo: AiPromptExecInfo = AiPromptExecInfo(),
    var outputInfo: AiPromptOutputInfo = AiPromptOutputInfo(null)
) : AiPromptTraceSupport(promptInfo, modelInfo, execInfo) {

    /** Unique identifier for this trace. */
    var uuid = randomUUID().toString()

    override fun toString() =
        "AiPromptTrace(promptInfo=$promptInfo, modelInfo=$modelInfo, execInfo=$execInfo, outputInfo=$outputInfo)"

}

/** Common elements of a prompt trace. */
abstract class AiPromptTraceSupport(
    var promptInfo: AiPromptInfo,
    var modelInfo: AiPromptModelInfo,
    var execInfo: AiPromptExecInfo = AiPromptExecInfo()
)
