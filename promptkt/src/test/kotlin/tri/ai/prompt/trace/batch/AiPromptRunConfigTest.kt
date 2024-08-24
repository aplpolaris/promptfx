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

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.openai.jsonWriter
import tri.ai.pips.PrintMonitor
import tri.ai.pips.RetryExecutor
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo

class AiPromptRunConfigTest {

    private val defaultTextCompletion = TextPlugin.textCompletionModels().first()

    private val promptInfo = AiPromptInfo(
        "Translate {{text}} into French.",
        mapOf("text" to "Hello, world!")
    )
    private val modelInfo = AiPromptModelInfo(
        "not a model",
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )
    private val modelInfo2 = AiPromptModelInfo(
        defaultTextCompletion.modelId,
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )

    @Test
    fun testExecute() {
        runBlocking {
            val runConfig = AiPromptRunConfig(promptInfo, modelInfo)
            val trace = RetryExecutor().execute(runConfig.task("test-task-id"), mapOf(), PrintMonitor()).values
            println("Trace: $trace")
            println("Trace: ${jsonWriter.writeValueAsString(trace)}")
        }
    }

    @Test
    @Tag("openai")
    fun testExecute2() {
        runBlocking {
            val runConfig = AiPromptRunConfig(promptInfo, modelInfo2)
            val trace = RetryExecutor().execute(runConfig.task("test-task-id"), mapOf(), PrintMonitor()).firstValue!!
            println("Trace: $trace")
            println("Trace: ${jsonWriter.writeValueAsString(trace)}")
        }
    }

}
