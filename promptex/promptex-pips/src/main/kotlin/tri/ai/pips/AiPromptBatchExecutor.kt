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
package tri.ai.pips

import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.tool.ExecContext
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiTaskInputInfo
import tri.ai.prompt.trace.AiTaskTrace
import tri.ai.prompt.trace.batch.AiPromptBatch
import tri.ai.prompt.trace.batch.AiPromptRunConfig
import tri.ai.prompt.trace.PromptInfo.Companion.filled

/**
 * Generate executable list of tasks for a prompt batch.
 * These can be passed to [AiWorkflowExecutor] for execution.
 */
fun AiPromptBatch.tasks(modelLookup: (String) -> TextChat) =
    runConfigs(modelLookup).mapIndexed { i, v -> v.task("$id $i") }

/** Get list of tasks for executing this batch of prompts. */
fun AiPromptBatch.plan(modelLookup: (String) -> TextChat): AiTaskBuilder<List<AiOutput?>> {
    val batchTasks = tasks(modelLookup)
    val batchId = id
    require(batchTasks.map { it.id }.toSet().size == batchTasks.size) { "Duplicate task IDs" }
    val finalTask = object : AiTask<Any?, List<AiOutput?>>("promptBatch", dependencies = batchTasks.map { it.id }.toSet()) {
        override suspend fun execute(input: Any?, context: ExecContext): List<AiOutput?> {
            val outputs = dependencies.map { dep -> context.get(dep) as AiOutput? }.toList()
            val nonNullOutputs = outputs.filterNotNull()
            val trace = AiTaskTrace(
                callerId = batchId,
                output = if (nonNullOutputs.isEmpty()) null else AiOutputInfo(nonNullOutputs)
            )
            context.logTrace(id, trace)
            return outputs
        }
    }
    return AiTaskBuilder(batchTasks, finalTask)
}

/** Create task for executing a run config. */
fun AiPromptRunConfig.task(id: String) = object : AiTask<Any?, AiOutput?>(id) {
    override suspend fun execute(input: Any?, context: ExecContext): AiOutput? = try {
        val trace = execute(modelLookup(modelInfo.modelId))
        context.logTrace(id, trace)
        trace.output?.outputs?.firstOrNull()
    } catch (x: NoSuchElementException) {
        val trace = AiTaskTrace(
            env = tri.ai.prompt.trace.AiEnvInfo.of(modelInfo),
            input = AiTaskInputInfo.of(promptInfo),
            exec = AiExecInfo.error("Model not found: ${modelInfo.modelId}")
        )
        context.logTrace(id, trace)
        null
    }
}

/**
 * Executes a text chat completion for this run config using [chat].
 * Does not mutate [AiPromptRunConfig.modelInfo]; the actual model ID used is taken from [chat].
 */
private suspend fun AiPromptRunConfig.execute(chat: TextChat): tri.ai.prompt.trace.AiPromptTrace {
    val promptText = promptInfo.filled()
    val result = chatWithModelInfo(chat, promptText, modelInfo)
    return result.copy(input = AiTaskInputInfo.of(promptInfo)).mapOutput { AiOutput.Text(it.message!!.content!!) }
}

private suspend fun chatWithModelInfo(chat: TextChat, text: String, modelInfo: AiModelInfo) =
    chat.chat(
        messages = listOf(TextChatMessage.user(text)),
        tokens = modelInfo.modelParams[AiModelInfo.MAX_TOKENS] as? Int,
        variation = modelInfo.toVariation(),
        stop = modelInfo.modelParams[AiModelInfo.STOP] as? List<String>
            ?: (modelInfo.modelParams[AiModelInfo.STOP] as? String)?.let { listOf(it) },
        numResponses = modelInfo.modelParams[AiModelInfo.NUM_RESPONSES] as? Int
    )

private fun AiModelInfo.toVariation() = MChatVariation(
    seed = (modelParams[AiModelInfo.SEED] as? Number)?.toInt(),
    temperature = modelParams[AiModelInfo.TEMPERATURE] as? Double,
    topP = modelParams[AiModelInfo.TOP_P] as? Double,
    presencePenalty = modelParams[AiModelInfo.PRESENCE_PENALTY] as? Double,
    frequencyPenalty = modelParams[AiModelInfo.FREQUENCY_PENALTY] as? Double,
)
