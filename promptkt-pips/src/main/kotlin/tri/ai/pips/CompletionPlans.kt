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

/** Planner that generates a plan to fill inputs into a prompt. */
fun CompletionBuilder.taskPlan(completion: TextCompletion) =
    aitask(id ?: "text-completion") { execute(completion) }.planner

/** Planner that generates a plan to fill inputs into a prompt. */
fun CompletionBuilder.taskPlan(chat: TextChat) =
    aitask(id ?: "text-chat") { execute(chat) }.planner