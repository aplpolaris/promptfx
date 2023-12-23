/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.prompt

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.openai.mapper

class AiPromptTraceTest {

    private val DEFAULT_TEXT_COMPLETION = TextPlugin.textCompletionModels().first()
    private val WRITER = mapper.writerWithDefaultPrettyPrinter()

    val config = AiPromptConfig().apply {
        prompt = "Translate {{text}} into French."
        promptParams = mapOf("text" to "Hello, world!")
    }
    val runSeries = AiPromptRunSeries().apply {
        model = DEFAULT_TEXT_COMPLETION.modelId
        prompt = listOf("Translate {{text}} into French.", "Translate {{text}} into German.")
        promptParams = mapOf("text" to "Hello, world!")
        runs = 2
    }

    @Test
    fun testSerializeConfig() {
        println(WRITER.writeValueAsString(config))
    }

    @Test
    fun testSerializeTrace() {
        println(WRITER.writeValueAsString(AiPromptTrace(config, "test output")))
        println(WRITER.writeValueAsString(AiPromptTrace.error(config, "test error")))
    }

    @Test
    fun testSerializeRunConfig() {
        println(WRITER.writeValueAsString(runSeries))
    }

    @Test
    @Disabled("Requires OpenAI API key")
    fun testExecute() {
        runBlocking {
            val trace = DEFAULT_TEXT_COMPLETION.run(config)
            println("Trace: $trace")
            println("Trace: ${WRITER.writeValueAsString(trace)}")
        }
    }

    @Test
    @Disabled("Requires OpenAI API key")
    fun testExecuteList() {
        runBlocking {
            runSeries.execute().onEach {
                println("Trace: $it")
                println("Trace: ${WRITER.writeValueAsString(it)}")
            }
        }
    }

}
