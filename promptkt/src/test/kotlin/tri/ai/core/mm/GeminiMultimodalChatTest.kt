package tri.ai.core.mm

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.gemini.GeminiClient
import tri.ai.gemini.GeminiModelIndex
import tri.util.BASE64_IMAGE_SAMPLE

@Tag("gemini")
class GeminiMultimodalChatTest {

    val client = GeminiClient.INSTANCE
    val chat = GeminiMultimodalChat(GeminiModelIndex.GEMINI_15_FLASH, client)

    @Test
    fun testChat_Simple() = runTest {
        val request = chatMessage {
            text("What is 2+3?")
        }
        val response = chat.chat(request)
        val responseText = response.firstValue.content[0].text!!
        println(responseText)
    }

    @Test
    fun testChat_Multiple() = runTest {
        val request = chatMessage {
            text("Random fruit?")
        }
        val params = MChatParameters(numResponses = 2)
        val response = chat.chat(request, params)
        val responseText = response.values!!.map { it.content[0].text!! }
        assertEquals(2, responseText.size) // NOTE - Gemini does not support multiple responses
        println(responseText)
    }

    @Test
    fun testChat_Image() = runTest {
        val request = chatMessage {
            text("According to the chart, how big is an apple?")
            inlineData(BASE64_IMAGE_SAMPLE)
        }
        val response = chat.chat(request)
        val responseText = response.firstValue.content[0].text!!
        println(responseText)
    }

}