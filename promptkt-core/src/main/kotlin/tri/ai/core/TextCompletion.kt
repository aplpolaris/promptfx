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
package tri.ai.core

import tri.ai.core.MChatVariation.Companion.temp
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptTemplate.Companion.defaultInputParams
import tri.ai.prompt.PromptTemplate.Companion.defaultInstructParams
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.PromptInfo
import tri.ai.prompt.trace.PromptInfo.Companion.filled

/** Interface for text completion. */
interface TextCompletion {

    val modelId: String

    /** Completes user text. */
    suspend fun complete(
        text: String,
        variation: MChatVariation = MChatVariation(),
        tokens: Int? = 1000,
        stop: List<String>? = null,
        numResponses: Int? = 1
    ): AiPromptTrace

}

//region ALTERNATE EXECUTIONS

/** Generate a task that adds user input to a prompt. */
suspend fun TextCompletion.promptTask(prompt: PromptDef, input: String, tokenLimit: Int, temp: Double?, stop: List<String>? = null, numResponses: Int? = null): AiPromptTrace {
    val promptInfo = PromptInfo(prompt.template!!, defaultInputParams(input))
    return promptTask(promptInfo, tokenLimit, temp, stop, numResponses)
}

/** Generate a task that completes a prompt. */
suspend fun TextCompletion.promptTask(promptText: String, tokenLimit: Int, temp: Double?, stop: List<String>? = null, numResponses: Int? = null) =
    promptTask(PromptInfo(promptText), tokenLimit, temp, stop, numResponses)

/** Generate a task that completes a prompt. */
suspend fun TextCompletion.promptTask(promptInfo: PromptInfo, tokenLimit: Int, temp: Double?, stop: List<String>? = null, numResponses: Int? = null) =
    complete(promptInfo.filled(), temp(temp), tokenLimit, stop, numResponses).copy(
        promptInfo = promptInfo
    )

/** Generate a task that combines a single instruction or question about contextual text. */
suspend fun TextCompletion.instructTask(prompt: PromptDef, instruct: String, userText: String, tokenLimit: Int, temp: Double?, numResponses: Int? = null) =
    templateTask(prompt, defaultInstructParams(input = userText, instruct = instruct), tokenLimit, temp, numResponses)

/** Generate a task that fills inputs into a prompt. */
suspend fun TextCompletion.templateTask(prompt: PromptDef, fields: Map<String, Any>, tokenLimit: Int, temp: Double?, numResponses: Int? = null) =
    promptTask(PromptInfo(prompt.template!!, fields), tokenLimit, temp, null, numResponses)

/** Generate a task that fills inputs into a prompt. */
suspend fun TextCompletion.templateTask(prompt: PromptDef, vararg fields: Pair<String, Any>, tokenLimit: Int, temp: Double?, numResponses: Int? = null): AiPromptTrace =
    templateTask(prompt, mapOf(*fields), tokenLimit, temp, numResponses)

//endregion
