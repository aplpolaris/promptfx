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

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.api.response.ResponseInput
import com.aallam.openai.api.response.ResponseRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.DataModality
import tri.ai.core.ModelLifecycle
import tri.ai.core.ModelType

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
    @Tag("openai")
    fun `test model config recommendations`() {
        runTest {
            val indexModels = OpenAiModelIndex.modelInfoIndex.values
                .filter { it.lifecycle !in setOf(ModelLifecycle.DEPRECATED, ModelLifecycle.DISCONTINUED) }
                .filter { it.type !in setOf(ModelType.MODERATION, ModelType.TEXT_EMBEDDING) }
                .sortedBy { it.id }

            println("=".repeat(80))
            println("Model Configuration Recommendations")
            println("=".repeat(80))

            var typeChangesNeeded = 0

            indexModels.forEach { modelInfo ->
                val supportsResponses = runCatching {
                    client.response(ResponseRequest(
                        model = ModelId(modelInfo.id),
                        input = ResponseInput("1+1="),
                        maxOutputTokens = 5
                    ))
                }.isSuccess

                val supportsChat = runCatching {
                    client.chatCompletion(ChatCompletionRequest(
                        model = ModelId(modelInfo.id),
                        messages = listOf(ChatMessage(ChatRole.User, "1+1=")),
                        maxTokens = 5
                    ))
                }.isSuccess

                val supportsCompletions = runCatching {
                    client.completion(CompletionRequest(
                        model = ModelId(modelInfo.id),
                        prompt = "1+1=",
                        maxTokens = 5
                    ))
                }.isSuccess

                val hasNonTextInput = modelInfo.inputs?.any { it != DataModality.text } == true
                val hasNonTextOutput = modelInfo.outputs?.any { it != DataModality.text } == true

                val recommendedType = when {
                    supportsCompletions && !supportsChat && !supportsResponses -> ModelType.TEXT_COMPLETION
                    (supportsChat || supportsResponses) && !hasNonTextInput -> ModelType.TEXT_CHAT
                    supportsChat && !supportsResponses -> ModelType.TEXT_VISION_CHAT
                    supportsResponses -> ModelType.RESPONSES
                    else -> null
                }

                if (recommendedType != null && recommendedType != modelInfo.type) {
                    println("\nModel: ${modelInfo.id}")
                    println("  Current type:     ${modelInfo.type}")
                    println("  Recommended type: $recommendedType")
                    println("  API support: completions=$supportsCompletions, chat=$supportsChat, responses=$supportsResponses")
                    println("  Inputs: ${modelInfo.inputs}, Outputs: ${modelInfo.outputs}")
                    typeChangesNeeded++
                }
            }

            println()
            if (typeChangesNeeded == 0) {
                println("No type changes needed.")
            } else {
                println("$typeChangesNeeded model(s) may need a type update.")
            }
            println("=".repeat(80))
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
