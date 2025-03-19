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
package tri.ai.gemini

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.MChatRole
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.gemini.GeminiModelIndex.GEMINI_15_FLASH
import tri.util.BASE64_IMAGE_SAMPLE
import java.net.URI

class GeminiVisionLanguageChatTest {

    companion object {
        lateinit var client: GeminiVisionLanguageChat

        @JvmStatic
        @BeforeAll
        fun setUp() {
            client = GeminiVisionLanguageChat(modelId = GEMINI_15_FLASH)
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            client.client.close()
        }
    }

    @Test
    @Tag("gemini")
    fun testChat() = runTest {
        val message = VisionLanguageChatMessage(
            MChatRole.User,
            "Describe this image in 6 words.",
            URI.create(BASE64_IMAGE_SAMPLE)
        )
        val resp = client.chat(listOf(message))
        println(resp.output!!.outputs.first())
    }

}
