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
package tri.ai.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.embedding.EmbeddingRequest
import com.aallam.openai.api.embedding.EmbeddingResponse
import com.aallam.openai.api.model.ModelId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.embedding.cosineSimilarity

@OptIn(BetaOpenAI::class)
@Disabled("Requires apikey")
class OpenAiTest {

    val client = OpenAiClient.INSTANCE.client

    @Test
    fun testChat() = runTest {
        val res = client.chatCompletion(
            ChatCompletionRequest(
                ModelId("gpt-3.5-turbo"),
                listOf(ChatMessage(ChatRole.User, "Give me a haiku about Kotlin."))
            )
        )
        println(res.choices[0].message!!.content!!.trim())
    }

    @Test
    fun testEmbedding() = runTest {
        val res = client.embeddings(
            EmbeddingRequest(
                ModelId(EMBEDDING_ADA),
                listOf(
                    "Give me a haiku about Kotlin.",
                    "Tell me a joke.",
                    "Input text to get embeddings for, encoded as a string or array of tokens. To get embeddings for multiple inputs in a single request, pass an array of strings or array of token arrays. Each input must not exceed 8192 tokens in length.",
                    "Thrusting playing biting"
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
