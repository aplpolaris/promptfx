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
import tri.ai.pips.api.PPlan
import tri.ai.pips.api.PPlanExecutor
import tri.ai.pips.core.ChatExecutable
import tri.ai.pips.core.ExecutableRegistry
import tri.ai.pips.core.MergedExecutableRegistry
import tri.ai.pips.core.PromptLibraryExecutableRegistry
import tri.ai.prompt.PromptLibrary

/** Pipeline config for [StarshipUi]. */
open class StarshipPipelineConfig(val chatEngine: TextChat) {
    /** Input generator. */
    open val generator: () -> String = { runBlocking { StarshipContentConfig.randomQuestion() } }
    
    /** JSON-based pipeline configuration. */
    val pipeline: PPlan by lazy {
        loadPipelineConfig()
    }
    
    /** Allow specifying a different pipeline configuration file. */
    protected open val pipelineConfigPath: String = "/tri/util/ui/starship/resources/starship-default-pipeline.json"
    
    /** Executable registry for the pipeline. */
    val executableRegistry: ExecutableRegistry by lazy {
        MergedExecutableRegistry(listOf(
            PromptLibraryExecutableRegistry(PromptLibrary.INSTANCE),
            ExecutableRegistry.create(listOf(ChatExecutable(chatEngine)))
        ))
    }
    
    /** Pipeline executor. */
    val pipelineExecutor: PPlanExecutor by lazy {
        PPlanExecutor(executableRegistry)
    }
    
    private fun loadPipelineConfig(): PPlan {
        return try {
            val resourceStream = javaClass.getResourceAsStream(pipelineConfigPath)
            val configJson = resourceStream?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Could not load starship pipeline configuration from $pipelineConfigPath")
            PPlan.parse(configJson)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load starship pipeline configuration from $pipelineConfigPath", e)
        }
    }
    
    // Legacy support - kept for backward compatibility
    /** Primary prompt template, with {{input}} and other parameters. */
    val primaryPrompt = PromptWithParams("docs-map/summarize")
    /** Executor for primary prompt. */
    var promptExec: AiPromptExecutor = object : AiPromptExecutor {
        override suspend fun exec(prompt: PromptWithParams, input: String): StarshipInterimResult {
            val response = CompletionBuilder()
                .prompt(prompt.prompt)
                .paramsInput(input)
                .execute(chatEngine).firstValue.textContent()
            return StarshipInterimResult(prompt.prompt.name ?: prompt.prompt.id, FormattedText(response), null, listOf())
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
