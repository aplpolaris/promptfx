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
package tri.ai.pips

import tri.ai.core.*
import tri.ai.prompt.AiPrompt

/** Planner that generates a plan for a single completion prompt. */
fun TextCompletion.promptPlan(prompt: AiPrompt, input: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null, history: List<TextChatMessage> = listOf()) =
    aitask(prompt.templateName) {
        promptTask(prompt, input, tokenLimit, temp, stop, numResponses, history)
    }.planner

/** Planner that generates a plan for a single instruction or question about user's text. */
fun TextCompletion.instructTextPlan(prompt: AiPrompt, instruct: String, userText: String, tokenLimit: Int, temp: Double?, numResponses: Int? = null, history: List<TextChatMessage> = listOf()) =
    aitask(prompt.template) {
        instructTask(prompt, instruct, userText, tokenLimit, temp, numResponses, history)
    }.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(prompt: AiPrompt, vararg fields: Pair<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null, numResponses: Int? = null, history: List<TextChatMessage> = listOf()) =
    aitask(prompt.templateName) {
        templateTask(prompt, fields.toMap(), tokenLimit, temp, requestJson, numResponses, history)
    }.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(prompt: AiPrompt, fields: Map<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null, numResponses: Int? = null, history: List<TextChatMessage> = listOf()) =
    aitask(prompt.templateName) {
        templateTask(prompt, fields, tokenLimit, temp, requestJson, numResponses, history)
    }.planner
