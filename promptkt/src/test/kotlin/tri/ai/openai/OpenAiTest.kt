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

    val client = OpenAiSettings.client

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
