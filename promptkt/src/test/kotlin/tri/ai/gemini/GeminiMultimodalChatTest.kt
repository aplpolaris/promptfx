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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MChatParameters
import tri.ai.core.TextChatMessage
import tri.ai.core.TextChatRole
import tri.ai.core.chatMessage
import tri.ai.gemini.GeminiClientTest.Companion
import tri.ai.gemini.GeminiModelIndex.GEMINI_15_FLASH
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
        assertEquals(1, responseText.size) { "Gemini only supports a single response" }
        println(responseText)
    }

    @Test
    fun testChat_Roles() = runTest {
        val request = listOf(
            chatMessage {
                text("You are a wizard that always responds as if you are casting a spell.")
                role(TextChatRole.System)
            },
            chatMessage {
                text("What should I have for dinner?")
                role(TextChatRole.User)
            }
        )
        val response = chat.chat(request)
        val responseText = response.firstValue.content[0].text!!
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
