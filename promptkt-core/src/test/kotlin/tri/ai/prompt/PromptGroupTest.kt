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

class PromptGroupTest {

    @Test
    fun testResolved_infer_from_group() {
        val group = PromptGroup("test", "1.x", PromptGroupDefaults("test-cat", listOf("tag1", "tag2")))
        val prompt = PromptDef("id", template = "", tags = listOf("tag2", "tag3")).resolved(group)
        assertEquals("test-cat", prompt.category)
        assertEquals("id", prompt.name)
        assertEquals("1.x", prompt.version)
        assertEquals(listOf("tag1", "tag2", "tag3"), prompt.tags)
    }

    @Test
    fun testResolved_infer_from_id() {
        val group = PromptGroup("test", "1.x", PromptGroupDefaults("test-cat", listOf("tag1", "tag2")))
        val prompt2 = PromptDef("alpha/beta@2.x", template = "").resolved(group)
        assertEquals("alpha", prompt2.category)
        assertEquals("beta", prompt2.name)
        assertEquals("2.x", prompt2.version)
        assertEquals(listOf("tag1", "tag2"), prompt2.tags)
        assertEquals(listOf<PromptArgDef>(), prompt2.args)
    }

    @Test
    fun testResolved_args() {
        val prompt3 = PromptDef("id", template = "{{alpha}} {{beta}}").resolved(PromptGroup(""))
        assertEquals(2, prompt3.args.size)
    }

}

