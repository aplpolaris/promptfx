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

import com.aallam.openai.api.response.ResponseInput
import com.aallam.openai.api.response.ResponseInputItem
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.*
import tri.ai.openai.OpenAiResponsesChat.Companion.buildResponseRequest

class OpenAiResponsesChatTest {

    private val client = OpenAiAdapter.INSTANCE

    //region MODEL INDEX TESTS

    @Test
    fun testResponsesModelsInIndex() {
        val responsesModels = OpenAiModelIndex.responsesModels()
        assertTrue(responsesModels.isNotEmpty(), "Responses models list should not be empty")
        assertTrue(responsesModels.contains("o3-pro"), "Should include o3-pro")
        assertTrue(responsesModels.contains("o1-pro"), "Should include o1-pro")
    }

    @Test
    fun testResponsesModelsHaveCorrectType() {
        val responsesModels = OpenAiModelIndex.responsesModels()
        responsesModels.forEach { modelId ->
            val info = OpenAiModelIndex.modelInfoIndex[modelId]
            assertNotNull(info, "Model info should exist for $modelId")
            assertEquals(tri.ai.core.ModelType.RESPONSES, info?.type, "Model $modelId should have type RESPONSES")
        }
    }

    //endregion

    //region REQUEST BUILDING TESTS

    @Test
    fun testBuildResponseRequest_SingleUserMessage() {
        val messages = listOf(chatMessage { text("What is 2+3?") })
        val request = buildResponseRequest("o3-pro", messages, MChatParameters())
        assertEquals("o3-pro", request.model.id)
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
        val request = buildResponseRequest("o1-pro", messages, MChatParameters())
        assertEquals("o1-pro", request.model.id)
        assertEquals("You are a helpful assistant.", request.instructions)
    }

    @Test
    fun testBuildResponseRequest_MultiTurnConversation() {
        val messages = listOf(
            chatMessage { text("Hello"); role(MChatRole.User) },
            chatMessage { text("Hi there!"); role(MChatRole.Assistant) },
            chatMessage { text("How are you?"); role(MChatRole.User) }
        )
        val request = buildResponseRequest("o3-pro", messages, MChatParameters())
        assertEquals("o3-pro", request.model.id)
        assertNull(request.instructions)
    }

    @Test
    fun testBuildResponseRequest_WithParameters() {
        val messages = listOf(chatMessage { text("Hello") })
        val params = MChatParameters(tokens = 500, variation = MChatVariation(temperature = 0.7))
        val request = buildResponseRequest("o3-pro", messages, params)
        assertEquals(500, request.maxOutputTokens)
        assertEquals(0.7, request.temperature)
    }

    //endregion

    //region LIVE API TESTS (require openai tag)

    @Test
    @Tag("openai")
    fun testChat_Simple() {
        val chat = OpenAiResponsesChat("o1-pro", client = client)
        chat.testChat_Simple()
    }

    @Test
    @Tag("openai")
    fun testChat_Roles() {
        val chat = OpenAiResponsesChat("o1-pro", client = client)
        chat.testChat_Roles()
    }

    //endregion

}
