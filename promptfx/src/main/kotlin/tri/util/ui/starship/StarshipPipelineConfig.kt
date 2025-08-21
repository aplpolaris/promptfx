/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.util.ui.starship

import kotlinx.coroutines.runBlocking
import tri.ai.core.CompletionBuilder
import tri.ai.core.TextChat
import tri.ai.text.docs.FormattedText

/** Pipeline config for [StarshipUi]. */
class StarshipPipelineConfig(val chatEngine: TextChat) {
    /** Input generator. */
    val generator: () -> String = { runBlocking { StarshipContentConfig.randomQuestion() } }
    /** Primary prompt template, with {{input}} and other parameters. */
    val primaryPrompt = PromptWithParams("docs-map/summarize")
    /** Executor for primary prompt. */
    var promptExec: AiPromptExecutor = object : AiPromptExecutor {
        override suspend fun exec(prompt: PromptWithParams, input: String): StarshipInterimResult {
            val response = CompletionBuilder()
                .prompt(prompt.prompt)
                .paramsInput(input)
                .execute(chatEngine).firstValue.content
            return StarshipInterimResult(prompt.prompt.name ?: prompt.prompt.id, FormattedText(response ?: "no response"), null, listOf())
        }
    }
    /** Secondary prompt executors. */
    val secondaryPrompts: List<PromptWithParams> = StarshipContentConfig.promptInfo.map {
        when (it) {
            is String -> PromptWithParams(it)
            is Map<*, *> -> PromptWithParams(it.keys.first() as String, it.values.first() as Map<String, Any>)
            else -> throw IllegalArgumentException("Invalid secondary prompt info: $it")
        }
    }
    /** Executor for secondary prompts. */
    var secondaryPromptExec: AiPromptExecutor = promptExec
}
