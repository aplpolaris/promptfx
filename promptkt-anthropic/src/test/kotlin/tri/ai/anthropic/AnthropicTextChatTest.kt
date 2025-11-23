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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole

class AnthropicTextChatTest {

    val client = AnthropicTextChat(AnthropicModelIndex.CLAUDE_3_5_HAIKU_20241022)

    @Test
    @Tag("anthropic")
    fun testChat() {
        runTest {
            val messages = listOf(
                TextChatMessage(MChatRole.System, "You are a helpful assistant."),
                TextChatMessage(MChatRole.User, "What is the capital of France?")
            )
            val res = client.chat(messages, tokens = 100)
            println(res)
            assertTrue("paris" in res.lowercase())
        }
    }

}
