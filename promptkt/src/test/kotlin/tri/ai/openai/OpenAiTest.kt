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
package tri.ai.openai

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.embedding.EmbeddingRequest
import com.aallam.openai.api.embedding.EmbeddingResponse
import com.aallam.openai.api.model.ModelId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.embedding.cosineSimilarity
import tri.ai.openai.OpenAiModelIndex.EMBEDDING_ADA
import tri.ai.openai.OpenAiModelIndex.GPT35_TURBO_ID
import tri.util.BASE64_IMAGE_SAMPLE

class OpenAiTest {

    val client = OpenAiClient.INSTANCE.client

    @Test
    fun testModelLibrary() {
        println(OpenAiModelIndex.MODEL_INFO_INDEX)
    }

    @Test
    @Tag("openai")
    fun testModels() = runTest {
        val res = client.models()
        println(res)
        println("-".repeat(50))
        println("OpenAI API models not in local index: " +
                (res.map { it.id.id }.toSet() - OpenAiModelIndex.MODEL_INFO_INDEX.values.map { it.id }.toSet()))
        println("-".repeat(50))
        println("Local index models not in OpenAI API: " +
                (OpenAiModelIndex.MODEL_INFO_INDEX.values.map { it.id }.toSet() - res.map { it.id.id }.toSet()))
    }

    @Test
    @Tag("openai")
    fun testChat() = runTest {
        val res = client.chatCompletion(
            ChatCompletionRequest(
                ModelId("gpt-3.5-turbo"),
                listOf(userMessage {
                    content = "Give me a haiku about Kotlin."
                })
            )
        )
        println(res.choices[0].message.content!!.trim())
    }

    @Test
    @Tag("openai")
    fun testChatMultiple() = runTest {
        val res = client.chatCompletion(
            ChatCompletionRequest(
                ModelId(GPT35_TURBO_ID),
                listOf(userMessage {
                    content = "Give me a haiku about Kotlin."
                }),
                n = 2
            )
        )
        println(res.choices)
    }

    @Test
    @Tag("openai")
    fun testChatImage() = runTest {
        val res = client.chatCompletion(
            chatCompletionRequest {
                model = ModelId("gpt-4-turbo")
                maxTokens = 2000
                messages {
                    user {
                        content {
                            text("Describe everything you see in this image. Provide a detailed list of contents.")
                            image(BASE64_IMAGE_SAMPLE)
                        }
                    }
                }
            }
        )
        println(res.choices[0].message.content!!.trim())
    }

    @Test
    @Tag("openai")
    fun testEmbedding() = runTest {
        val res = client.embeddings(
            EmbeddingRequest(
                ModelId(EMBEDDING_ADA),
                listOf(
                    "Give me a haiku about Kotlin.",
                    "Tell me a joke.",
                    "Input text to get embeddings for, encoded as a string or array of tokens. To get embeddings for multiple inputs in a single request, pass an array of strings or array of token arrays. Each input must not exceed 8192 tokens in length.",
                    "Playing bridge on a starship"
                )
            )
        )
        // ada size is 1536
        println("Embedding size = ${res.embeddings[0].embedding.size}")
        res.embeddings.forEach { println(it.embedding) }
        val max = res.embeddings.size - 1
        (0..max).forEach { i ->
            (i+1..max).forEach { j ->
                println("Similarity between $i and $j: ${res.similarity(i, j)}")
            }
        }
    }

    private fun EmbeddingResponse.similarity(i: Int, j: Int): String {
        val resp1 = embeddings[i].embedding
        val resp2 = embeddings[j].embedding
        val sim = cosineSimilarity(resp1, resp2)
        return "%.2f".format(sim)
    }

}
