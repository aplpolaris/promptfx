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
package tri.ai.prompt.trace

import org.junit.jupiter.api.Test
import tri.util.json.jsonWriter

class AiPromptTraceTest {

    private val promptInfo = PromptInfo(
        "Translate {{text}} into French.",
        mapOf("text" to "Hello, world!")
    )
    private val modelInfo = AiModelInfo(
        "not a model",
        modelParams = mapOf("maxTokens" to 100, "temperature" to 0.5, "stop" to "}")
    )

    @Test
    fun testSerializePromptInfo() {
        println(jsonWriter.writeValueAsString(promptInfo))
    }

    @Test
    fun testSerializeTrace() {
        println(jsonWriter.writeValueAsString(AiPromptTrace(promptInfo, modelInfo, AiExecInfo(), AiOutputInfo.text("test output"))))
        println(jsonWriter.writeValueAsString(AiPromptTrace(promptInfo, modelInfo, AiExecInfo.error("test error"))))
    }

}
