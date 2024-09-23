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
package tri.ai.pips

import tri.ai.core.TextCompletion
import tri.ai.core.instructTask
import tri.ai.core.promptTask
import tri.ai.core.templateTask

/** Planner that generates a plan for a single completion prompt. */
fun TextCompletion.promptPlan(promptId: String, input: String, tokenLimit: Int, temp: Double?, stop: String? = null, numResponses: Int? = null) =
    aitask<String>(promptId) {
        promptTask(promptId, input, tokenLimit, temp, stop, numResponses)
    }.planner

/** Planner that generates a plan for a single instruction or question about user's text. */
fun TextCompletion.instructTextPlan(promptId: String, instruct: String, userText: String, tokenLimit: Int, temp: Double?, numResponses: Int? = null) =
    aitask<String>(promptId) {
        instructTask(promptId, instruct, userText, tokenLimit, temp, numResponses)
    }.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(promptId: String, vararg fields: Pair<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null, numResponses: Int? = null) =
    aitask<String>(promptId) {
        templateTask(promptId, fields.toMap(), tokenLimit, temp, requestJson, numResponses)
    }.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun TextCompletion.templatePlan(promptId: String, fields: Map<String, String>, tokenLimit: Int, temp: Double?, requestJson: Boolean? = null, numResponses: Int? = null) =
    aitask<String>(promptId) {
        templateTask(promptId, fields, tokenLimit, temp, requestJson, numResponses)
    }.planner
