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
package tri.ai.prompt.trace.batch

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.openai.jsonMapper
import tri.ai.openai.jsonWriter
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.PrintMonitor
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceDatabase

class AiPromptBatchTest {

    private val defaultTextCompletion = TextPlugin.textCompletionModels().first()

    private val batch = AiPromptBatchCyclic("test-batch-languages").apply {
        model = defaultTextCompletion.modelId
        prompt = listOf("Translate {{text}} into {{language}}.")
        promptParams = mapOf("text" to "Hello, world!", "language" to listOf("French", "German"))
        runs = 2
    }

    @Test
    fun testSerialize() {
        println(jsonWriter.writeValueAsString(batch))
    }

    @Test
    @Tag("openai")
    fun testExecute() {
        runBlocking {
            AiPipelineExecutor.execute(batch.tasks(), PrintMonitor()).results.values.onEach {
                println("AiTaskResult with nested AiPromptTrace:\n${jsonWriter.writeValueAsString(it)}")
            }
        }
    }

    @Test
    @Tag("openai")
    fun testBatchExecuteDatabase() {
        runBlocking {
            val batch = AiPromptBatchCyclic.repeat("test-batch-repeat",
                AiPromptInfo("Generate a random number between 1 and 100."),
                AiPromptModelInfo(defaultTextCompletion.modelId),
                4
            )
            val result = AiPipelineExecutor.execute(batch.tasks(), PrintMonitor())
            val db = AiPromptTraceDatabase().apply {
                addTraces(result.results.values.map { it.firstValue!! as AiPromptTrace })
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

}
