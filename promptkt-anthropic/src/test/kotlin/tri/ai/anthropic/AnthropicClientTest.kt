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

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@Tag("anthropic")
class AnthropicClientTest {

    companion object {
        lateinit var client: AnthropicClient

        @JvmStatic
        @BeforeAll
        fun setUp() {
            client = AnthropicClient()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            client.close()
        }
    }

    @Test
    fun testListModels() {
        val models = client.listModels()
        assertTrue(models.isNotEmpty())
        println("Available models:")
        models.forEach { model ->
            println("  ${model.id()}")
        }
    }

    @Test
    fun testCreateMessage() {
        val response = client.createMessage(
            "Write a haiku about coding.",
            AnthropicModelIndex.CLAUDE_3_5_HAIKU_20241022,
            maxTokens = 100
        )
        assertNotNull(response)
        println(response.content())
        assertTrue(response.content().isNotEmpty())
    }

}
