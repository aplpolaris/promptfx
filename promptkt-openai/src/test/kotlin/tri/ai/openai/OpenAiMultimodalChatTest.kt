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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.testChat_Simple
import tri.ai.core.testChat_Multiple
import tri.ai.core.testChat_Roles
import tri.ai.core.testChat_Image
import tri.ai.core.testChat_Tools

class OpenAiMultimodalChatTest {

    private val client = OpenAiAdapter.INSTANCE
    private val chat = OpenAiMultimodalChat(OpenAiModelIndex.GPT35_TURBO_ID, client)
    private val chatVision = OpenAiMultimodalChat(OpenAiModelIndex.GPT4_TURBO_ID, client)

    @Test
    fun canLoadMultimodalChatTestViaReflection() {
        val clazz = Class.forName("tri.ai.core.MultimodalChatTestKt")
        println("Loaded: $clazz from " + clazz.protectionDomain.codeSource?.location)
    }

    @Test
    @Tag("openai")
    fun testChat_Simple() {
        chat.testChat_Simple()
    }

    @Test
    @Tag("openai")
    fun testChat_Multiple() {
        chat.testChat_Multiple {
            assertEquals(2, it.size)
        }
    }

    @Test
    @Tag("openai")
    fun testChat_Roles() {
        chat.testChat_Roles()
    }

    @Test
    @Tag("openai")
    fun testChat_Image() {
        chatVision.testChat_Image()
    }

    @Test
    @Tag("openai")
    fun testChat_Tools() {
        chat.testChat_Tools()
    }

}
