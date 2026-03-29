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
package tri.ai.anthropic

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MChatVariation
import tri.ai.core.MChatRole
import tri.ai.core.TextChatMessage

class AnthropicTextChatTest {

    val client = AnthropicTextChat()

    @Test
    @Tag("anthropic")
    fun testChat() {
        runTest {
            val res = client.chat(
                listOf(TextChatMessage(MChatRole.User, "Translate 'Hello, world!' into French.")),
                variation = MChatVariation(temperature = 0.5),
                tokens = 100
            )
            println(res)
            assertTrue("monde" in res.firstValue.textContent().lowercase())
        }
    }

    @Test
    @Tag("anthropic")
    fun testChatWithSystemMessage() {
        runTest {
            val res = client.chat(
                listOf(
                    TextChatMessage(MChatRole.System, "You are a helpful assistant that always responds in French."),
                    TextChatMessage(MChatRole.User, "Say hello.")
                ),
                variation = MChatVariation(temperature = 0.5),
                tokens = 100
            )
            println(res)
            assertTrue(res.firstValue.textContent().isNotBlank())
        }
    }
}
