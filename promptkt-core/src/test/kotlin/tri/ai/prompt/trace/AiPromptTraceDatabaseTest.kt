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
package tri.ai.prompt.trace

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tri.util.jackson.jsonWriter

class AiPromptTraceDatabaseTest {

    private val promptInfo = PromptInfo(
        "Translate {{text}} into French.",
        mapOf("text" to "Hello, world!")
    )
    private val modelInfo = AiModelInfo(
        "not a model",
        mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )
    private val execInfo1 = AiExecInfo()
    private val execInfo2 = AiExecInfo.error("test error")
    private val outputInfo1 = AiOutputInfo.text("test output")
    private val outputInfo2 = AiOutputInfo(listOf())

    @Test
    fun testSerialize() {
        val db = AiPromptTraceDatabase().apply {
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo1, outputInfo1))
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo1, outputInfo1))
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo2, outputInfo2))
            addTrace(AiPromptTrace(promptInfo, modelInfo, execInfo1, outputInfo2))
        }
        assertEquals(4, db.traces.size)
        assertEquals(1, db.prompts.size)
        assertEquals(1, db.models.size)
        assertEquals(2, db.execs.size)
        assertEquals(2, db.outputs.size)

        println(jsonWriter.writeValueAsString(db))
    }

}
