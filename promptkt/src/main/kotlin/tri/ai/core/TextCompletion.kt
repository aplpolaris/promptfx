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
package tri.ai.core

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.openai.OpenAiCompletionChat
import tri.ai.openai.jsonMapper
import tri.ai.prompt.AiPrompt
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Interface for text completion. */
interface TextCompletion {

    val modelId: String

    /** Completes user text. */
    suspend fun complete(
        text: String,
        tokens: Int? = 150,
        temperature: Double? = null,
        stop: String? = null,
        numResponses: Int? = 1
    ): AiPromptTrace<String>

}

//region ALTERNATE EXECUTIONS

/** Generate a task that adds user input to a prompt. */
suspend fun TextCompletion.promptTask(prompt: AiPrompt, input: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null): AiPromptTrace<String> {
    val promptParams = AiPrompt.inputParams(input)
    val promptInfo = AiPromptInfo(prompt.template, promptParams)
    return promptTask(promptInfo, tokenLimit, temp, stop, numResponses)
}

/** Generate a task that completes a prompt. */
suspend fun TextCompletion.promptTask(promptText: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) =
    promptTask(AiPromptInfo(promptText), tokenLimit, temp, stop, numResponses)

/** Generate a task that completes a prompt. */
suspend fun TextCompletion.promptTask(promptInfo: AiPromptInfo, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) =
    complete(promptInfo.filled(), tokenLimit, temp, stop, numResponses = numResponses).copy(
        promptInfo = promptInfo
    )

/** Generate a task that combines a single instruction or question about contextual text. */
suspend fun TextCompletion.instructTask(prompt: AiPrompt, instruct: String, userText: String, tokenLimit: Int, temp: Double?, numResponses: Int? = null): AiPromptTrace<String> {
    val promptParams = AiPrompt.instructParams(instruct = instruct, input = userText)
    return complete(prompt.fill(promptParams), tokenLimit, temp, numResponses = numResponses).copy(
        promptInfo = AiPromptInfo(prompt.template, promptParams)
    )
}

/** Generate a task that fills inputs into a prompt. */
suspend fun TextCompletion.templateTask(prompt: AiPrompt, fields: Map<String, Any>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = false, numResponses: Int? = null): AiPromptTrace<String> {
    return prompt.fill(fields).let {
        if (this is OpenAiCompletionChat)
            complete(it, tokenLimit, temp, null, requestJson, numResponses)
        else
            complete(it, tokenLimit, temp, null, numResponses)
    }.copy(
        promptInfo = AiPromptInfo(prompt.template, fields)
    )
}

/** Generate a task that fills inputs into a prompt. */
suspend fun TextCompletion.templateTask(prompt: AiPrompt, vararg fields: Pair<String, Any>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = false, numResponses: Int? = null): AiPromptTrace<String> =
    templateTask(prompt, mapOf(*fields), tokenLimit, temp, requestJson, numResponses)

/** Generate a task that adds user input to a prompt, and attempt to convert the result to json if possible. */
suspend inline fun <reified T> TextCompletion.jsonPromptTask(prompt: AiPrompt, input: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) =
    promptTask(prompt, input, tokenLimit, temp, stop, numResponses).let {
        it.mapOutput {
            try {
                jsonMapper.readValue<T>(it.trim())
            } catch (x: JsonMappingException) {
                null
            }
        }
    }

//endregion
