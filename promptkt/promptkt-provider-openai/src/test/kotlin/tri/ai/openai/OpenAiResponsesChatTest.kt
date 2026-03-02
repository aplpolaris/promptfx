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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.*
import tri.ai.openai.OpenAiResponsesChat.Companion.buildResponseRequest

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

    //endregion

}
