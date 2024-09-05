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
package tri.ai.openai

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.prompt.AiPromptLibrary
import tri.ai.core.TextCompletion
import tri.ai.pips.aitask
import tri.ai.prompt.trace.*

/** Generate a task that adds user input to a prompt. */
suspend fun TextCompletion.promptTask(promptId: String, input: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null): AiPromptTrace<String> {
    val prompt = AiPromptLibrary.lookupPrompt(promptId)
    val promptParams = prompt.promptParams(input)
    val promptInfo = AiPromptInfo(prompt.template, promptParams)
    return promptTask(promptInfo, tokenLimit, temp, stop, numResponses)
}

/** Generate a task that completes a prompt. */
suspend fun TextCompletion.promptTask(promptText: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) =
    promptTask(AiPromptInfo(promptText), tokenLimit, temp, stop, numResponses)

/** Generate a task that completes a prompt. */
suspend fun TextCompletion.promptTask(promptInfo: AiPromptInfo, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) =
    complete(promptInfo.filled(), tokenLimit, temp, stop, numResponses = numResponses)

/** Generate a task that combines a single instruction or question about contextual text. */
suspend fun TextCompletion.instructTask(promptId: String, instruct: String, userText: String, tokenLimit: Int, temp: Double?, numResponses: Int? = null): AiPromptTrace<String> {
    val prompt = AiPromptLibrary.lookupPrompt(promptId)
    val promptParams = prompt.instructParams(instruct = instruct, input = userText)
    return complete(prompt.fill(promptParams), tokenLimit, temp, numResponses = numResponses)
}

/** Generate a task that fills inputs into a prompt. */
suspend fun TextCompletion.templateTask(promptId: String, fields: Map<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean?, numResponses: Int?): AiPromptTrace<String> {
    return AiPromptLibrary.lookupPrompt(promptId).fill(fields).let {
        if (this is OpenAiCompletionChat)
            complete(it, tokenLimit, temp, null, requestJson, numResponses)
        else
            complete(it, tokenLimit, temp, null, numResponses)
    }
}

private fun <X> mapOfNotNull(vararg pairs: Pair<X, Any?>) =
    pairs.filter { it.second != null }
        .associate { it.first to it.second!! }

//region CONVERTING TASKS

/** Generate a task that adds user input to a prompt, and attempt to convert the result to json if possible. */
suspend inline fun <reified T> TextCompletion.jsonPromptTask(id: String, input: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) =
    promptTask(id, input, tokenLimit, temp, stop, numResponses).let {
        it.mapOutput {
            try {
                jsonMapper.readValue<T>(it.trim())
            } catch (x: JsonMappingException) {
                null
            }
        }
    }

//endregion

//region PLANNERS

/** Planner that generates a plan for a single completion prompt. */
fun TextCompletion.promptPlan(promptId: String, input: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) = aitask(promptId) {
    promptTask(promptId, input, tokenLimit, temp, stop, numResponses)
}.planner

/** Planner that generates a plan for a single instruction or question about user's text. */
fun TextCompletion.instructTextPlan(promptId: String, instruct: String, userText: String, tokenLimit: Int, temp: Double?, numResponses: Int? = null) = aitask(promptId) {
    instructTask(promptId, instruct, userText, tokenLimit, temp, numResponses)
}.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(promptId: String, vararg fields: Pair<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null, numResponses: Int? = null) = aitask<String>(promptId) {
    templateTask(promptId, fields.toMap(), tokenLimit, temp, requestJson, numResponses)
}.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(promptId: String, fields: Map<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null, numResponses: Int? = null) = aitask<String>(promptId) {
    templateTask(promptId, fields, tokenLimit, temp, requestJson, numResponses)
}.planner

//endregion
