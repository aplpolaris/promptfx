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

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiPromptTraceDatabase
import tri.ai.prompt.trace.PromptInfo
import tri.ai.prompt.trace.batch.AiPromptBatchCyclic
import tri.ai.prompt.trace.batch.AiPromptRunConfig
import tri.util.json.jsonMapper
import tri.util.json.jsonWriter

class AiPromptBatchExecutorTest {

    private val defaultTextCompletion = TextPlugin.textCompletionModels().first()

    private val batch = AiPromptBatchCyclic("test-batch-languages").apply {
        model = defaultTextCompletion.modelId
        prompt = listOf("Translate {{text}} into {{language}}.")
        promptParams = mapOf("text" to "Hello, world!", "language" to listOf("French", "German"))
        runs = 2
    }

    private val promptInfo = PromptInfo(
        "Translate {{text}} into French.",
        mapOf("text" to "Hello, world!")
    )
    private val modelInfo = AiModelInfo(
        "not a model",
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )
    private val modelInfo2 = AiModelInfo(
        defaultTextCompletion.modelId,
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )

    @Test
    @Tag("openai")
    fun testExecute() {
        runBlocking {
            AiPipelineExecutor.execute(batch.tasks { TextPlugin.chatModel(it) }, PrintMonitor()).interimResults.values.onEach {
                println("AiTaskResult with nested AiPromptTrace:\n${jsonWriter.writeValueAsString(it)}")
            }
        }
    }

    @Test
    @Tag("openai")
    fun testBatchExecuteDatabase() {
        runBlocking {
            val batch = AiPromptBatchCyclic.repeat("test-batch-repeat",
                PromptInfo("Generate a random number between 1 and 100."),
                AiModelInfo(defaultTextCompletion.modelId),
                4
            )
            val result = AiPipelineExecutor.execute(batch.tasks { TextPlugin.chatModel(it) }, PrintMonitor())
            val db = AiPromptTraceDatabase().apply {
                addTraces(result.interimResults.values)
            }
            val output = jsonWriter.writeValueAsString(db)
            val db2 = jsonMapper.readValue<AiPromptTraceDatabase>(output)
            Assertions.assertEquals(db.traces, db2.traces)
            Assertions.assertEquals(db.prompts, db2.prompts)
            Assertions.assertEquals(db.models, db2.models)
            Assertions.assertEquals(db.execs, db2.execs)
            println(jsonWriter.writeValueAsString(db2))
        }
    }

    @Test
    fun testExecute_RunConfig() {
        runBlocking {
            val runConfig = AiPromptRunConfig(promptInfo, modelInfo)
            val trace = RetryExecutor().execute(runConfig.task("test-task-id"), mapOf(), PrintMonitor()).values
            println("Trace: $trace")
            println("Trace: ${jsonWriter.writeValueAsString(trace)}")
        }
    }

    @Test
    @Tag("openai")
    fun testExecute_RunConfig2() {
        runBlocking {
            val runConfig = AiPromptRunConfig(promptInfo, modelInfo2)
            val trace = RetryExecutor().execute(runConfig.task("test-task-id"), mapOf(), PrintMonitor()).firstValue
            println("Trace: $trace")
            println("Trace: ${jsonWriter.writeValueAsString(trace)}")
        }
    }
}
