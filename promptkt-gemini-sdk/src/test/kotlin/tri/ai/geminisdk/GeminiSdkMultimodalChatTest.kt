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
package tri.ai.geminisdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.*

@Tag("gemini-sdk")
class GeminiSdkMultimodalChatTest {

    val client = GeminiSdkClient.INSTANCE
    val chat = GeminiSdkMultimodalChat(GeminiSdkModelIndex.GEMINI_25_FLASH_LITE, client)

    @Test
    @Tag("gemini-sdk")
    fun testChat_Simple() {
        chat.testChat_Simple()
    }

    @Test
    @Tag("gemini-sdk")
    fun testChat_Multiple() {
        chat.testChat_Multiple {
            assertEquals(1, it.size) { "Gemini only supports a single response" }
        }
    }

    @Test
    @Tag("gemini-sdk")
    fun testChat_Roles() {
        chat.testChat_Roles()
    }

    @Test
    @Tag("gemini-sdk")
    fun testChat_Image() {
        chat.testChat_Image()
    }

    @Test
    @Tag("gemini-sdk")
    fun testChat_Tools() {
        chat.testChat_Tools()
    }

}
