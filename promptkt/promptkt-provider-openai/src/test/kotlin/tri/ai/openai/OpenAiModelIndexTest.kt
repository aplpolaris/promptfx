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
package tri.ai.openai

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class OpenAiModelIndexTest {

    val client = OpenAiAdapter.INSTANCE.client

    @Test
    fun testModelLibrary() {
        println(OpenAiModelIndex.modelInfoIndex)
    }

    @Test
    @Tag("openai")
    fun `test what we see about models in the API`() {
        runTest {
            val res = client.models()
            println("Models from OpenAI API:")
            println("-".repeat(98))
            println("| ${"ID".padEnd(40)} | ${"Owned By".padEnd(15)} | Created    | ${"Permission".padEnd(20)} |")
            println("-".repeat(98))
            res.sortedByDescending { it.created!! }.forEach {
                val dmy = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(it.created!! * 1000L))
                // print table with columns: id, ownedBy, created (formatted as YYYY-MM-DD), and permission (if not null)
                // id: 40 chars, ownedBY: 15 chars, created: 10 chars, permission: 20 chars, pipe-separated
                println("| ${it.id.id.padEnd(40)} | ${it.ownedBy!!.padEnd(15)} | $dmy | ${(it.permission?.joinToString(",") { it.id } ?: "N/A").padEnd(20)} |")
            }
            println("-".repeat(98))
        }
    }

    @Test
    @Tag("openai")
    fun `test comparing model index and models from API`() {
        runTest {
            val res = client.models()
            val apiModelIds = res.map { it.id.id }.toSet()
            val indexIds = OpenAiModelIndex.modelInfoIndex.values.map { it.id }.toSet()
            println("-".repeat(50))
            println("OpenAI API models not in local index:")
            val ids = apiModelIds - indexIds
            ids.sorted().forEach {
                // use regex to identify and skip model ids with suffix like "-####" or "-####-##-##"
                if (!it.matches(Regex(".*-\\d{4}(-\\d{2}(-\\d{2})?)?\$")))
                    println("  $it")
            }
            println("-".repeat(50))
            println("Local index models not in OpenAI API:")
            val ids2 = indexIds - apiModelIds
            ids2.sorted().forEach {
                println("  $it")
            }
        }
    }

    @Test
    fun testModels() {
        println(OpenAiModelIndex.audioModels())
        println(OpenAiModelIndex.chatModels())
        println(OpenAiModelIndex.chatModelsInclusive())
        println(OpenAiModelIndex.completionModels())
        println(OpenAiModelIndex.embeddingModels())
        println(OpenAiModelIndex.imageGeneratorModels())
        println(OpenAiModelIndex.moderationModels())
        println(OpenAiModelIndex.multimodalModels())
        println(OpenAiModelIndex.responsesModels())
        println(OpenAiModelIndex.ttsModels())
        println(OpenAiModelIndex.visionLanguageModels())
    }

}
