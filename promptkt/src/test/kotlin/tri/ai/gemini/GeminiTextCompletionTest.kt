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
package tri.ai.gemini

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.gemini.GeminiModelIndex.GEMINI_15_FLASH

class GeminiTextCompletionTest {

    val client = GeminiTextCompletion(GEMINI_15_FLASH)

    @Test
    @Tag("gemini")
    fun testComplete() = runTest {
        val res = client.complete("Translate Hello, world! into French.", 100, 0.5)
        println(res)
        assertTrue("monde" in res.firstValue.lowercase())
    }

    @Test
    @Tag("gemini")
    fun testCompleteMultiple() = runTest {
        val res = client.complete("Translate Hello, world! into French.", 100, 0.5, numResponses = 2)
        assertEquals(1, res.output!!.outputs.size) { "Gemini only supports a single response" }
        println(res)
    }

}
