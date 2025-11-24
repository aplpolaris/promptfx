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

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.gemini.GeminiClientTest.Companion.client

class GeminiModelIndexTest {

    @Test
    fun testModels() {
        assertTrue(GeminiModelIndex.javaClass.getResourceAsStream("resources/gemini-models.yaml") != null)

        println(GeminiModelIndex.audioModels())
        println(GeminiModelIndex.chatModels())
        println(GeminiModelIndex.completionModels())
        println(GeminiModelIndex.embeddingModels())
        println(GeminiModelIndex.multimodalModels())
        println(GeminiModelIndex.moderationModels())
        println(GeminiModelIndex.ttsModels())
        println(GeminiModelIndex.imageGeneratorModels())
        println(GeminiModelIndex.visionLanguageModels())

        assertTrue(GeminiModelIndex.multimodalModels().isNotEmpty())

        // no additional model info has been configured
        assertTrue(GeminiModelIndex.modelInfoIndex.isEmpty())
    }

    @Test
    @Tag("gemini")
    fun testIndexModels() {
        runBlocking {
            val res = GeminiClient().listModels().models
            val apiModelIds = res.map { it.name.substringAfter("/") }.toSet()
            val indexIds = GeminiModelIndex.modelIds
            println(res)
            println("-".repeat(50))
            println("Gemini API models not in local index:")
            val ids = apiModelIds - indexIds
            ids.sorted().forEach {
                // use regex to identify and skip model ids with suffix like "-####" or "-####-##-##" or "-##-##"
                if (!it.matches(Regex(".*-\\d{4}(-\\d{2}(-\\d{2})?)?")))
                    println("  $it")
            }
            println("-".repeat(50))
            println("Local index models not in Gemini API: " + (indexIds - apiModelIds))
        }
    }

}
