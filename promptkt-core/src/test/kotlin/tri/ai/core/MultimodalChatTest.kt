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
package tri.ai.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import tri.util.BASE64_IMAGE_SAMPLE

fun MultimodalChat.testChat_Simple() = runTest {
    val request = chatMessage {
        text("What is 2+3?")
    }
    val response = chat(request)
    val responseText = response.firstValue.textContent()
    println(responseText)
}

fun MultimodalChat.testChat_Multiple(responseCheck: (List<String>) -> Unit) = runTest {
    val request = chatMessage {
        text("Random fruit?")
    }
    val params = MChatParameters(numResponses = 2)
    val response = chat(request, params)
    val responseText = response.values!!.map { it.textContent() }
    responseCheck(responseText)
    println(responseText)
}

fun MultimodalChat.testChat_Roles() = runTest {
    val request = listOf(
        chatMessage {
            text("You are a wizard that always responds as if you are casting a spell.")
            role(MChatRole.System)
        },
        chatMessage {
            text("What should I have for dinner?")
            role(MChatRole.User)
        }
    )
    val response = chat(request)
    val responseText = response.firstValue.textContent()
    println(responseText)
}

fun MultimodalChat.testChat_Image() = runTest {
    val request = chatMessage {
        text("According to the chart, how big is an apple?")
        image(BASE64_IMAGE_SAMPLE)
    }
    val response = chat(request)
    val responseText = response.firstValue.textContent()
    println(responseText)
}

fun MultimodalChat.testChat_Tools() = runTest {
    val query = chatMessage {
        text("Convert 5 to a Roman numeral.")
    }
    val params = MChatParameters(tools = MChatTools(tools = listOf(
        MTool("RomanNumeral", "Converts numbers to Roman numerals",
            """{"type":"object","properties":{"input":{"type":"integer","description":"the number to convert"}}}""")
    )))

    // in the first call, the AI will provide a response indicating what tool to use
    val toolCallMessage = chat(query, params).firstValue.multimodalMessage!!
    assertEquals(MChatRole.Assistant, toolCallMessage.role)
    assertTrue(toolCallMessage.content.isNullOrEmpty())

    val calls = toolCallMessage.toolCalls!!
    assertEquals(1, calls.size)
    assertEquals("RomanNumeral", calls[0].name)
    assertTrue("""{"input":"5"}""" == calls[0].argumentsAsJson || """{"input":5}""" == calls[0].argumentsAsJson)
    assertEquals(null, toolCallMessage.toolCallId)

    // in the second call, we add the tool response to the chat
    val toolResultMessage = MultimodalChatMessage.tool("V", calls[0].id.ifEmpty { calls[0].name })
    val finalResponse = chat(listOf(query, toolCallMessage, toolResultMessage), params)

    println(finalResponse.firstValue.multimodalMessage!!.content!![0].text)
    assertTrue(finalResponse.firstValue.multimodalMessage!!.content!![0].text!!.contains("V"))
}
