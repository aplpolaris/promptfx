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
package tri.ai.tool

import com.aallam.openai.api.logging.LogLevel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.openai.OpenAiAdapter
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO
import tri.ai.tool.JsonToolTest.Companion.SAMPLE_TOOLS

@Tag("openai")
class JsonToolExecutorTest {

    @Test
    fun testTools() {
        OpenAiAdapter.INSTANCE.settings.logLevel = LogLevel.None

        runBlocking {
            JsonToolExecutor(OpenAiAdapter.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("Multiply 21 times 2 and then convert it to Roman numerals.")

            JsonToolExecutor(OpenAiAdapter.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("Convert 5 to a Roman numeral.")

            JsonToolExecutor(OpenAiAdapter.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("What year was Jurassic Park?")
        }
    }

}
