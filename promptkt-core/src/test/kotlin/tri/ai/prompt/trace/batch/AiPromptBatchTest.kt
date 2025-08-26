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
package tri.ai.prompt.trace.batch

import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.openai.jsonWriter

class AiPromptBatchTest {

    private val batch = AiPromptBatchCyclic("test-batch-languages").apply {
        model = TextPlugin.textCompletionModels().first().modelId
        prompt = listOf("Translate {{text}} into {{language}}.")
        promptParams = mapOf("text" to "Hello, world!", "language" to listOf("French", "German"))
        runs = 2
    }

    @Test
    fun testSerialize() {
        println(jsonWriter.writeValueAsString(batch))
    }

}
