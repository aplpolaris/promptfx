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
import tri.ai.core.MChatParameters
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.testChat_Simple
import tri.ai.core.testChat_Roles

class OpenAiResponsesChatTest {

    private val client = OpenAiAdapter.INSTANCE
    private val chat = OpenAiResponsesChat("o3-pro", client)

    @Test
    fun testResponsesChatCreation() {
        assertEquals("o3-pro", chat.modelId)
    }

    @Test
    fun testResponsesRequestBuilding_simpleText() {
        val messages = listOf(MultimodalChatMessage.user("What is 2+3?"))
        val params = MChatParameters(tokens = 100)
        val request = OpenAiResponsesChat.responsesRequest("o3-pro", messages, params)
        assertNotNull(request)
        assertEquals("o3-pro", request.model.id)
        assertEquals(100, request.maxOutputTokens)
    }

    @Test
    fun testResponsesRequestBuilding_withSystemMessage() {
        val messages = listOf(
            MultimodalChatMessage.text(MChatRole.System, "You are a helpful assistant."),
            MultimodalChatMessage.user("What is 2+3?")
        )
        val params = MChatParameters(tokens = 100)
        val request = OpenAiResponsesChat.responsesRequest("o1-pro", messages, params)
        assertNotNull(request)
        assertEquals("You are a helpful assistant.", request.instructions)
    }

    @Test
    @Tag("openai")
    fun testChat_Simple() {
        chat.testChat_Simple()
    }

    @Test
    @Tag("openai")
    fun testChat_Roles() {
        chat.testChat_Roles()
    }

}
