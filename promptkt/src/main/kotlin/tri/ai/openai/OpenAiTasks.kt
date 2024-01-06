/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
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

import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.prompt.AiPromptLibrary
import tri.ai.core.TextCompletion
import tri.ai.pips.aitask

/** Generate a task that adds user input to a prompt. */
suspend fun TextCompletion.promptTask(promptId: String, input: String, tokenLimit: Int, temp: Double?, stop: String? = null) =
    AiPromptLibrary.lookupPrompt(promptId).prompt(input).let {
        complete(it, tokenLimit, temp, stop = stop)
    }

/** Generate a task that combines a single instruction or question about contextual text. */
suspend fun TextCompletion.instructTask(promptId: String, instruct: String, userText: String, tokenLimit: Int, temp: Double?) =
    AiPromptLibrary.lookupPrompt(promptId).instruct(instruct, userText).let {
        complete(it, tokenLimit, temp)
    }

/** Generate a task that fills inputs into a prompt. */
suspend fun TextCompletion.templateTask(promptId: String, fields: Map<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean?) =
    AiPromptLibrary.lookupPrompt(promptId).fill(fields).let {
        if (this is OpenAiCompletionChat)
            complete(it, tokenLimit, temp, null, requestJson)
        else
            complete(it, tokenLimit, temp)
    }

//region CONVERTING TASKS

/** Generate a task that adds user input to a prompt, and attempt to convert the result to json if possible. */
suspend inline fun <reified T> TextCompletion.jsonPromptTask(id: String, input: String, tokenLimit: Int, temp: Double?) =
    promptTask(id, input, tokenLimit, temp).let {
        it.map { mapper.readValue<T>(it.trim()) }
    }

//endregion

//region PLANNERS

/** Planner that generates a plan for a single completion prompt. */
fun TextCompletion.promptPlan(promptId: String, input: String, tokenLimit: Int, temp: Double?, stop: String? = null) = aitask(promptId) {
    promptTask(promptId, input, tokenLimit, temp, stop)
}.planner

/** Planner that generates a plan for a single instruction or question about user's text. */
fun TextCompletion.instructTextPlan(promptId: String, instruct: String, userText: String, tokenLimit: Int, temp: Double?) = aitask(promptId) {
    instructTask(promptId, instruct, userText, tokenLimit, temp)
}.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(promptId: String, vararg fields: Pair<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null) = aitask(promptId) {
    templateTask(promptId, fields.toMap(), tokenLimit, temp, requestJson)
}.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(promptId: String, fields: Map<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null) = aitask(promptId) {
    templateTask(promptId, fields, tokenLimit, temp, requestJson)
}.planner

//endregion
