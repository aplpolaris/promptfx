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
package tri.ai.anthropicsdk

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MChatRole
import tri.ai.core.MChatParameters
import tri.ai.core.MChatVariation
import tri.ai.core.MultimodalChatMessage
import tri.ai.anthropicsdk.AnthropicSdkModelIndex.CLAUDE_3_5_SONNET

class AnthropicSdkMultimodalChatTest {

    val client = AnthropicSdkMultimodalChat(CLAUDE_3_5_SONNET, AnthropicSdkClient.INSTANCE)

    @Test
    @Tag("anthropic-sdk")
    fun testTextOnlyMultimodalChat() {
        runTest {
            val messages = listOf(
                MultimodalChatMessage.text(MChatRole.User, "Write a one-sentence summary of what Paris is famous for.")
            )
            val res = client.chat(messages, MChatParameters(tokens = 100, variation = MChatVariation()))
            println(res)
            val text = res.firstValue.textContent()
            assertTrue(text.isNotBlank())
            println("Response: $text")
        }
    }

}
