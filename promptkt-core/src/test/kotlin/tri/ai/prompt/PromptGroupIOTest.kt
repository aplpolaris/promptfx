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
package tri.ai.prompt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PromptGroupIOTest {

    @Test
    fun testPromptExists() {
        val group = PromptGroupIO.readFromResource("chat.yaml")
        assertEquals("chat", group.groupId) { "Prompt group ID should be 'chat'" }
        assert(group.prompts.isNotEmpty()) { "Prompt group should not be empty" }
        assertEquals("chat", group.defaults.category)
        assertEquals("chat-back", group.prompts[0].name)
    }

    @Test
    fun testResolved() {
        val group = PromptGroupIO.readFromResource("chat.yaml")
        val prompt = group.prompts[0]
        assertEquals("chat/chat-back@1.0.0", prompt.id)
        assertEquals("chat-back", prompt.name)
        assertEquals("chat", prompt.category)
        assertEquals("1.0.0", prompt.version)
    }

    @Test
    fun testReadAll() {
        val groups = PromptGroupIO.readAllFromResourceDirectory()
        assertEquals(19, groups.size)
    }

}
