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
package tri.ai.tool

import com.aallam.openai.api.logging.LogLevel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.openai.OpenAiClient
import tri.ai.openai.OpenAiModels.GPT35_TURBO

@Disabled("Requires apikey")
class JsonFunctionExecutorTest {

    companion object {
        val SAMPLE_TOOL1 = object : JsonTool("calc", "Use this to do math",
            """{"type":"object","properties":{"input":{"type":"string"}}}""") {
            override suspend fun run(input: JsonObject) = "42"
        }
        val SAMPLE_TOOL2 = object : JsonTool("romanize", "Converts numbers to Roman numerals",
            """{"type":"object","properties":{"input":{"type":"integer"}}}""") {
            override suspend fun run(input: JsonObject): String {
                val value = input["input"]?.jsonPrimitive?.int ?: throw RuntimeException("No input")
                return when (value) {
                    5 -> "V"
                    42 -> "XLII"
                    84 -> "LXXXIV"
                    else -> "I don't know"
                }
            }
        }
        val SAMPLE_TOOLS = listOf(SAMPLE_TOOL1, SAMPLE_TOOL2)
    }

    @Test
    fun testTools() {
        OpenAiClient.INSTANCE.settings.logLevel = LogLevel.None

        runBlocking {
            JsonFunctionExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("Multiply 21 times 2 and then convert it to Roman numerals.")

            JsonFunctionExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("Convert 5 to a Roman numeral.")

            JsonFunctionExecutor(OpenAiClient.INSTANCE, GPT35_TURBO, SAMPLE_TOOLS)
                .execute("What year was Jurassic Park?")
        }
    }

}
