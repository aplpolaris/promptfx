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
package tri.ai.anthropic

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import tri.ai.core.TextChatMessage

class AnthropicChatTest {

    @Test
    @Disabled("Requires API key")
    suspend fun testBasicChat() {
        val chat = AnthropicChat()
        val messages = listOf(
            TextChatMessage(MChatRole.User, "Hello, can you tell me about yourself?")
        )
        
        try {
            val result = chat.chat(messages, MChatVariation(), 100)
            println("Response: ${result.outputInfo.outputs.firstOrNull()?.text}")
        } catch (e: Exception) {
            println("Expected error without API key: ${e.message}")
        }
    }

}