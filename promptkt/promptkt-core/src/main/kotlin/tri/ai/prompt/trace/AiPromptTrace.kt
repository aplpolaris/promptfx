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
import tri.ai.core.TextChatMessage

/** Details of an executed prompt, including prompt configuration, model configuration, execution metadata, and output. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AiPromptTrace(
    promptInfo: PromptInfo? = null,
    modelInfo: AiModelInfo? = null,
    execInfo: AiExecInfo = AiExecInfo(),
    outputInfo: AiOutputInfo? = null
) : AiPromptTraceSupport(promptInfo, modelInfo, execInfo, outputInfo) {

    override fun toString() =
        "AiPromptTrace(promptInfo=$prompt, modelInfo=$model, execInfo=$exec, outputInfo=$output)"

    override fun copy(
        promptInfo: PromptInfo?,
        modelInfo: AiModelInfo?,
        execInfo: AiExecInfo
    ): AiPromptTrace = AiPromptTrace(promptInfo, modelInfo, execInfo, output)

    /** Convert output using a provided function, without modifying any other parts of the response. */
    fun mapOutput(transform: (AiOutput) -> AiOutput) = AiPromptTrace(
        prompt,
        model,
        exec,
        output?.map(transform)
    )

    companion object {
        /** Create an execution info with an error. */
        fun error(modelInfo: AiModelInfo?, message: String?, throwable: Throwable? = null, duration: Long? = null, durationTotal: Long? = null, attempts: Int? = null) =
            AiPromptTrace(null, modelInfo,
                AiExecInfo(message, throwable, responseTimeMillis = duration, responseTimeMillisTotal = durationTotal, attempts = attempts)
            )
        /** Task not attempted because input was invalid. */
        fun invalidRequest(modelId: String, message: String) =
            invalidRequest(AiModelInfo(modelId), message)
        /** Task not attempted because input was invalid. */
        fun  invalidRequest(modelInfo: AiModelInfo?, message: String) =
            error(modelInfo, message, IllegalArgumentException(message))

        /** Generate trace for single output. */
        fun output(output: AiOutput) = AiPromptTrace(outputInfo = AiOutputInfo(listOf(output)))

        /** Generate trace for single output. */
        fun output(output: String) = AiPromptTrace(outputInfo = AiOutputInfo.text(output))
        /** Generate trace for list of outputs. */
        fun output(output: List<String>) = AiPromptTrace(outputInfo = AiOutputInfo.text(output))
        /** Generate trace for single output message. */
        fun outputMessage(output: TextChatMessage) = AiPromptTrace(outputInfo = AiOutputInfo.message(output))

        /** Wraps a series of outputs within a single list output object. */
        fun outputListAsSingleResult(list: List<AiOutput>) =
            output(AiOutput(other = list.map { it.content() }))

    }

}

