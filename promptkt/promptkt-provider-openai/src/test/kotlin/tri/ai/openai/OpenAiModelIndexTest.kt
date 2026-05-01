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

    companion object {
        private val SNAPSHOT_MODEL_REGEX = Regex(".*-(\\d{4}|\\d{2}-\\d{2}|preview)$")
        private val IGNORED_MODEL_IDS = setOf(
            "gpt-3.5-turbo-16k",
            "chatgpt-image-latest",
            "gpt-4o-transcribe-diarize",
            "gpt-5-chat-latest",
            "gpt-5.1-chat-latest",
            "gpt-5.2-chat-latest",
            "gpt-audio",
            "gpt-audio-1.5",
            "gpt-audio-mini",
            "gpt-image-2",
            "gpt-realtime",
            "gpt-realtime-1.5",
            "gpt-realtime-mini",
            "o4-mini-deep-research",
            "sora-2",
            "sora-2-pro"
        )
        private val IGNORED_MODEL_SUFFIX_REGEX = listOf(
            Regex(".*-search-api$")
        )
    }

    @Test
    fun testModelLibrary() {
        OpenAiModelIndex.modelInfoIndex.forEach { (key, info) ->
            println("${info.id} >>>")
            println("  Type: ${info.type}")
            println("  Source: ${info.source}")

            println("  Name: ${info.metadata.name}")
            println("  Description: ${info.metadata.description?.trim()}")

            println("  Created: ${info.metadata.created}")
            println("  Version: ${info.metadata.version}")
            println("  Deprecation: ${info.metadata.deprecation}")
            println("  Lifecycle: ${info.metadata.lifecycle}")

            println("  Inputs: ${info.capabilities.inputs}")
            println("  Outputs: ${info.capabilities.outputs}")
            println("  Input Token Limit: ${info.params["inputTokenLimit"]}")
            println("  Output Token Limit: ${info.params["outputTokenLimit"]}")
            println("  Total Token Limit: ${info.params["totalTokenLimit"]}")

            println("  Output Dimension: ${info.params["outputDimension"]}")

            println("  Params: ${info.params}")
        }
    }

    private fun String.isIgnoredModelId(): Boolean {
        if (this in IGNORED_MODEL_IDS) return true
        if (matches(SNAPSHOT_MODEL_REGEX)) return true
        return IGNORED_MODEL_SUFFIX_REGEX.any { it.matches(this) }
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
            ids.sorted().filter { !it.isIgnoredModelId() }.forEach {
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

        println("Audio: " + OpenAiModelIndex.audioModels())
        println("Chat: " + OpenAiModelIndex.chatModels())
        println("Completion: " + OpenAiModelIndex.completionModels())
        println("Embedding: " + OpenAiModelIndex.embeddingModels())
        println("Image Generator: " + OpenAiModelIndex.imageGeneratorModels())
        println("Moderation: " + OpenAiModelIndex.moderationModels())
        println("Multimodal: " + OpenAiModelIndex.multimodalModels())
        println("Responses: " + OpenAiModelIndex.responsesModels())
        println("TTS: " + OpenAiModelIndex.ttsModels())
    }

}
