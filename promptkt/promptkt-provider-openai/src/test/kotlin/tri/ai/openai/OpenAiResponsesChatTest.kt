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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.*
import tri.ai.openai.OpenAiResponsesChat.Companion.buildResponseRequest
import tri.ai.openai.OpenAiResponsesChat.Companion.imageUrlJsonElement

class OpenAiResponsesChatTest {

    private val client = OpenAiAdapter.INSTANCE
    private val TEST_MODEL = "gpt-5-nano"

    //region MODEL INDEX TESTS

    @Test
    fun testResponsesModelsInIndex() {
        val responsesModels = OpenAiModelIndex.responsesModels()
        assertTrue(responsesModels.isNotEmpty(), "Responses models list should not be empty")
    }

    @Test
    fun testResponsesModelsHaveCorrectType() {
        val responsesModels = OpenAiModelIndex.responsesModels()
        responsesModels.forEach { modelId ->
            val info = OpenAiModelIndex.modelInfoIndex[modelId]
            assertNotNull(info, "Model info should exist for $modelId")
            assertEquals(ModelType.RESPONSES, info?.type, "Model $modelId should have type RESPONSES")
        }
    }

    //endregion

    //region REQUEST BUILDING TESTS

    @Test
    fun testBuildResponseRequest_SingleUserMessage() {
        val messages = listOf(chatMessage { text("What is 2+3?") })
        val request = buildResponseRequest(TEST_MODEL, messages, MChatParameters())
        assertEquals(TEST_MODEL, request.model.id)
        assertNull(request.instructions)
        val input = request.input
        assertNotNull(input)
    }

    @Test
    fun testBuildResponseRequest_SystemAndUserMessage() {
        val messages = listOf(
            chatMessage { text("You are a helpful assistant."); role(MChatRole.System) },
            chatMessage { text("What is 2+3?") }
        )
        val request = buildResponseRequest(TEST_MODEL, messages, MChatParameters())
        assertEquals(TEST_MODEL, request.model.id)
        assertEquals("You are a helpful assistant.", request.instructions)
    }

    @Test
    fun testBuildResponseRequest_MultiTurnConversation() {
        val messages = listOf(
            chatMessage { text("Hello"); role(MChatRole.User) },
            chatMessage { text("Hi there!"); role(MChatRole.Assistant) },
            chatMessage { text("How are you?"); role(MChatRole.User) }
        )
        val request = buildResponseRequest(TEST_MODEL, messages, MChatParameters())
        assertEquals(TEST_MODEL, request.model.id)
        assertNull(request.instructions)
    }

    @Test
    fun testBuildResponseRequest_WithParameters() {
        val messages = listOf(chatMessage { text("Hello") })
        val params = MChatParameters(tokens = 500, variation = MChatVariation(temperature = 0.7))
        val request = buildResponseRequest(TEST_MODEL, messages, params)
        assertEquals(500, request.maxOutputTokens)
        assertEquals(0.7, request.temperature)
    }

    @Test
    fun testBuildResponseRequest_WithTools() {
        val messages = listOf(chatMessage { text("Convert 5 to Roman numeral.") })
        val params = MChatParameters(tools = MChatTools(tools = listOf(
            MTool("RomanNumeral", "Converts numbers to Roman numerals",
                """{"type":"object","properties":{"input":{"type":"integer"}}}""")
        )))
        val request = buildResponseRequest(TEST_MODEL, messages, params)
        assertNotNull(request.tools)
        assertEquals(1, request.tools!!.size)
        assertEquals("RomanNumeral", request.tools!![0].name)
        assertNotNull(request.toolChoice)
    }

    @Test
    fun testBuildResponseRequest_WithJsonResponseFormat() {
        val messages = listOf(chatMessage { text("Hello") })
        val params = MChatParameters(responseFormat = MResponseFormat.JSON)
        val request = buildResponseRequest(TEST_MODEL, messages, params)
        assertNotNull(request.text)
    }

    @Test
    fun testBuildResponseRequest_WithToolResultMessages() {
        val toolCall = MToolCall(id = "call_123", name = "MyTool", argumentsAsJson = """{"input":5}""")
        val messages = listOf(
            chatMessage { text("Use the tool.") },
            MultimodalChatMessage(MChatRole.Assistant, toolCalls = listOf(toolCall)),
            MultimodalChatMessage.tool("result_value", "call_123")
        )
        val request = buildResponseRequest(TEST_MODEL, messages, MChatParameters())
        assertNotNull(request.input)
    }

    //endregion

    //region IMAGE ENCODING TESTS

    @Test
    fun testImageUrlJsonElement_PlainUrl() {
        val url = "https://example.com/image.png"
        val element = imageUrlJsonElement(url)
        assertTrue(element is JsonPrimitive, "Plain URL should be serialized as a JsonPrimitive")
        assertEquals(url, (element as JsonPrimitive).content)
    }

    @Test
    fun testImageUrlJsonElement_DataUri() {
        val dataUri = "data:image/png;base64,iVBORw0KGgo="
        val element = imageUrlJsonElement(dataUri)
        assertTrue(element is JsonPrimitive, "Data URI should be serialized as a JsonPrimitive string")
        assertEquals(dataUri, (element as JsonPrimitive).content)
    }

    @Test
    fun testBuildResponseRequest_WithImageUrl() {
        val imageUrl = "https://example.com/chart.png"
        val messages = listOf(
            chatMessage {
                text("Describe this image.")
                image(imageUrl)
            }
        )
        val request = buildResponseRequest(TEST_MODEL, messages, MChatParameters())
        val inputArray = request.input?.value as? JsonArray
        assertNotNull(inputArray, "Input should be a JsonArray")
        val firstMessage = inputArray!![0] as? JsonObject
        assertNotNull(firstMessage, "First item should be a JsonObject")
        val content = firstMessage!!["content"] as? JsonArray
        assertNotNull(content, "Content should be a JsonArray for multi-part messages")
        val imagePart = content!!.filterIsInstance<JsonObject>().firstOrNull {
            it["type"]?.jsonPrimitive?.content == "input_image"
        }
        assertNotNull(imagePart, "Should have an input_image content part")
        val imageUrlValue = imagePart!!["image_url"]
        assertTrue(imageUrlValue is JsonPrimitive, "Plain URL should serialize image_url as a JsonPrimitive")
        assertEquals(imageUrl, imageUrlValue!!.jsonPrimitive.content)
    }

    @Test
    fun testBuildResponseRequest_WithDataUriImage() {
        val dataUri = "data:image/png;base64,iVBORw0KGgo="
        val messages = listOf(
            chatMessage {
                text("Describe this image.")
                image(dataUri)
            }
        )
        val request = buildResponseRequest(TEST_MODEL, messages, MChatParameters())
        val inputArray = request.input?.value as? JsonArray
        assertNotNull(inputArray, "Input should be a JsonArray")
        val firstMessage = inputArray!![0] as? JsonObject
        assertNotNull(firstMessage, "First item should be a JsonObject")
        val content = firstMessage!!["content"] as? JsonArray
        assertNotNull(content, "Content should be a JsonArray for multi-part messages")
        val imagePart = content!!.filterIsInstance<JsonObject>().firstOrNull {
            it["type"]?.jsonPrimitive?.content == "input_image"
        }
        assertNotNull(imagePart, "Should have an input_image content part")
        val imageUrlValue = imagePart!!["image_url"]
        assertTrue(imageUrlValue is JsonPrimitive, "Data URI should serialize image_url as a plain JsonPrimitive string")
        assertEquals(dataUri, imageUrlValue!!.jsonPrimitive.content)
    }

    //endregion

    //region LIVE API TESTS (require openai tag)

    @Test
    @Tag("openai")
    fun testChat_Simple() {
        val chat = OpenAiResponsesChat(TEST_MODEL, client = client)
        chat.testChat_Simple()
    }

    @Test
    @Tag("openai")
    fun testChat_Roles() {
        val chat = OpenAiResponsesChat(TEST_MODEL, client = client)
        chat.testChat_Roles()
    }

    @Test
    @Tag("openai")
    fun testChat_Image() {
        val chat = OpenAiResponsesChat(TEST_MODEL, client = client)
        chat.testChat_Image()
    }

    @Test
    @Tag("openai")
    fun testChat_Tools() {
        val chat = OpenAiResponsesChat(TEST_MODEL, client = client)
        chat.testChat_Tools()
    }

    //endregion

}
