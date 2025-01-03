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
                            image(
                                "data:image/png;base64,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"
                            )
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
