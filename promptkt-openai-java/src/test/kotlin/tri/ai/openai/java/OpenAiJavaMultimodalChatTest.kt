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
package tri.ai.openai.java

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MChatParameters
import tri.ai.core.MChatVariation.Companion.temp
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatMessagePart
import tri.ai.openai.java.OpenAiJavaModelIndex.GPT_4O

class OpenAiJavaMultimodalChatTest {

    val client = OpenAiJavaMultimodalChat(GPT_4O)

    @Test
    @Tag("openai")
    fun testChat() {
        runTest {
            val res = client.chat(
                listOf(
                    MultimodalChatMessage(
                        MChatRole.User,
                        listOf(MChatMessagePart.text("What is 2+2? Answer with just the number."))
                    )
                ),
                MChatParameters(variation = temp(0.5), tokens = 50)
            )
            println(res)
            assertTrue(res.output != null)
        }
    }

}
