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
package tri.ai.prompt.trace

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage

class AiPromptMultiAttemptTest {

    // -------------------------------------------------------------------------
    // tryJsonStringList
    // -------------------------------------------------------------------------

    @Test
    fun `tryJsonStringList - clean JSON array`() {
        val output = AiOutput(text = """["apple", "banana", "cherry"]""")
        assertEquals(listOf("apple", "banana", "cherry"), output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - JSON object with first array field`() {
        val output = AiOutput(text = """{"items": ["alpha", "beta", "gamma"]}""")
        assertEquals(listOf("alpha", "beta", "gamma"), output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - jsonKey selects named field in JSON object`() {
        val output = AiOutput(text = """{"known_items": ["fox", "cat"], "items_in_input": ["fox", "dog"]}""")
        assertEquals(listOf("fox", "dog"), output.tryJsonStringList(jsonKey = "items_in_input"))
    }

    @Test
    fun `tryJsonStringList - jsonKey returns null when key not found`() {
        val output = AiOutput(text = """{"items": ["a", "b"]}""")
        assertNull(output.tryJsonStringList(jsonKey = "missing_key"))
    }

    @Test
    fun `tryJsonStringList - jsonKey ignored for plain JSON array`() {
        val output = AiOutput(text = """["apple", "banana"]""")
        assertEquals(listOf("apple", "banana"), output.tryJsonStringList(jsonKey = "ignored"))
    }

    @Test
    fun `tryJsonStringList - without jsonKey picks first array field (known_items before items_in_input)`() {
        // Demonstrates the bug that jsonKey fixes: without it, "known_items" is returned
        val output = AiOutput(text = """{"known_items": ["fox"], "items_in_input": ["fox", "dog"]}""")
        assertEquals(listOf("fox"), output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - markdown code fence`() {
        val output = AiOutput(text = "```json\n[\"one\", \"two\", \"three\"]\n```")
        assertEquals(listOf("one", "two", "three"), output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - markdown code fence no language tag`() {
        val output = AiOutput(text = "```\n[\"x\", \"y\"]\n```")
        assertEquals(listOf("x", "y"), output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - invalid JSON returns null`() {
        val output = AiOutput(text = "This is not JSON at all.")
        assertNull(output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - null text returns null`() {
        val output = AiOutput()
        assertNull(output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - JSON object with no array field returns null`() {
        val output = AiOutput(text = """{"key": "value"}""")
        assertNull(output.tryJsonStringList())
    }

    @Test
    fun `tryJsonStringList - JSON scalar returns null`() {
        val output = AiOutput(text = "42")
        assertNull(output.tryJsonStringList())
    }

    // -------------------------------------------------------------------------
    // mergeJsonLists - UNION
    // -------------------------------------------------------------------------

    @Test
    fun `mergeJsonLists UNION - deduplicates case-insensitively`() {
        val lists = listOf(
            listOf("Apple", "Banana"),
            listOf("banana", "Cherry"),
            listOf("APPLE", "Date")
        )
        val result = mergeJsonLists(lists, JsonListMergeStrategy.UNION)
        assertEquals(listOf("Apple", "Banana", "Cherry", "Date"), result)
    }

    @Test
    fun `mergeJsonLists UNION - single list`() {
        val lists = listOf(listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), mergeJsonLists(lists, JsonListMergeStrategy.UNION))
    }

    @Test
    fun `mergeJsonLists UNION - empty input`() {
        assertEquals(emptyList<String>(), mergeJsonLists(emptyList(), JsonListMergeStrategy.UNION))
    }

    // -------------------------------------------------------------------------
    // mergeJsonLists - INTERSECTION
    // -------------------------------------------------------------------------

    @Test
    fun `mergeJsonLists INTERSECTION - items in all attempts`() {
        val lists = listOf(
            listOf("Apple", "Banana", "Cherry"),
            listOf("banana", "cherry", "Date"),
            listOf("CHERRY", "banana", "Elderberry")
        )
        val result = mergeJsonLists(lists, JsonListMergeStrategy.INTERSECTION)
        // "banana" and "cherry" appear in all three; order follows first attempt
        assertEquals(listOf("Banana", "Cherry"), result)
    }

    @Test
    fun `mergeJsonLists INTERSECTION - no common items`() {
        val lists = listOf(
            listOf("Apple", "Banana"),
            listOf("Cherry", "Date")
        )
        assertTrue(mergeJsonLists(lists, JsonListMergeStrategy.INTERSECTION).isEmpty())
    }

    @Test
    fun `mergeJsonLists INTERSECTION - single list`() {
        val lists = listOf(listOf("a", "b"))
        assertEquals(listOf("a", "b"), mergeJsonLists(lists, JsonListMergeStrategy.INTERSECTION))
    }

    // -------------------------------------------------------------------------
    // mergeJsonLists - TOP_REPEATED
    // -------------------------------------------------------------------------

    @Test
    fun `mergeJsonLists TOP_REPEATED - majority threshold default 0_5`() {
        val lists = listOf(
            listOf("Apple", "Banana", "Cherry"),
            listOf("apple", "Date"),
            listOf("APPLE", "banana")
        )
        // "apple" appears in 3/3, "banana" in 2/3, "cherry" and "date" in 1/3
        val result = mergeJsonLists(lists, JsonListMergeStrategy.TOP_REPEATED)
        // minCount = max(1, floor(3 * 0.5)) = max(1, 1) = 1, so only items with count >= 1 are included
        // That means all items appear, but sorted by descending count
        assertTrue(result.indexOf("Apple") < result.indexOf("Banana"))
        assertTrue("Cherry" in result)
        assertTrue("Date" in result)
    }

    @Test
    fun `mergeJsonLists TOP_REPEATED - higher threshold filters low-frequency items`() {
        val lists = listOf(
            listOf("Apple", "Banana", "Cherry"),
            listOf("apple", "Date"),
            listOf("APPLE", "banana")
        )
        // minAttemptFraction=0.66 => minCount = max(1, floor(3*0.66)) = max(1,1) = 1
        // Use 1.0 to require all attempts
        val result = mergeJsonLists(lists, JsonListMergeStrategy.TOP_REPEATED, minAttemptFraction = 1.0)
        // minCount = max(1, floor(3*1.0)) = 3, only "apple" appears in all 3
        assertEquals(listOf("Apple"), result)
    }

    @Test
    fun `mergeJsonLists TOP_REPEATED - sorted by frequency`() {
        val lists = listOf(
            listOf("A", "B", "C"),
            listOf("a", "b"),
            listOf("A")
        )
        // A: 3 attempts, B: 2 attempts, C: 1 attempt
        val result = mergeJsonLists(lists, JsonListMergeStrategy.TOP_REPEATED, minAttemptFraction = 0.0)
        assertEquals(listOf("A", "B", "C"), result)
    }

    @Test
    fun `mergeJsonLists TOP_REPEATED - preserves original case from first occurrence`() {
        val lists = listOf(
            listOf("MyItem"),
            listOf("myitem"),
            listOf("MYITEM")
        )
        val result = mergeJsonLists(lists, JsonListMergeStrategy.TOP_REPEATED)
        assertEquals(1, result.size)
        assertEquals("MyItem", result[0])
    }

    @Test
    fun `mergeJsonLists TOP_REPEATED - empty input`() {
        assertEquals(emptyList<String>(), mergeJsonLists(emptyList(), JsonListMergeStrategy.TOP_REPEATED))
    }

    // -------------------------------------------------------------------------
    // executeMultiAttemptJsonList
    // -------------------------------------------------------------------------

    /** Creates a fake [TextChat] that returns successive [responses] in a round-robin. */
    private fun fakeChat(vararg responses: String): TextChat {
        val iter = generateSequence(0) { (it + 1) % responses.size }.iterator()
        return object : TextChat {
            override val modelId = "fake-model"
            override val modelSource = "fake"
            override suspend fun chat(
                messages: List<TextChatMessage>,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?,
                requestJson: Boolean?
            ) = AiPromptTrace.output(responses[iter.next()])
        }
    }

    private fun errorChat(): TextChat = object : TextChat {
        override val modelId = "error-model"
        override val modelSource = "fake"
        override suspend fun chat(
            messages: List<TextChatMessage>,
            variation: MChatVariation,
            tokens: Int?,
            stop: List<String>?,
            numResponses: Int?,
            requestJson: Boolean?
        ) = AiPromptTrace.error(modelInfo = null, message = "simulated error")
    }

    @Test
    fun `executeMultiAttemptJsonList - merges results across attempts`() = runTest {
        val chat = fakeChat(
            """["apple", "banana", "cherry"]""",
            """["apple", "date"]""",
            """["apple", "banana"]"""
        )
        val trace = tri.ai.core.CompletionBuilder()
            .text("list some fruits")
            .executeMultiAttemptJsonList(chat, attempts = 3, mergeStrategy = JsonListMergeStrategy.TOP_REPEATED)

        assertTrue(trace.exec.succeeded())
        assertEquals(3, trace.exec.attempts)
        val result = trace.values?.firstOrNull()?.other as? List<*>
        assertNotNull(result)
        // "apple" appears 3 times, "banana" 2 times; both should be present
        assertTrue(result!!.contains("apple"))
        assertTrue(result.contains("banana"))
    }

    @Test
    fun `executeMultiAttemptJsonList - sets requestJson flag`() = runTest {
        var capturedRequestJson: Boolean? = null
        val chat = object : TextChat {
            override val modelId = "spy-model"
            override val modelSource = "fake"
            override suspend fun chat(
                messages: List<TextChatMessage>,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?,
                requestJson: Boolean?
            ): AiPromptTrace {
                capturedRequestJson = requestJson
                return AiPromptTrace.output("""["a"]""")
            }
        }
        tri.ai.core.CompletionBuilder()
            .text("list things")
            .executeMultiAttemptJsonList(chat, attempts = 1)

        assertEquals(true, capturedRequestJson)
    }

    @Test
    fun `executeMultiAttemptJsonList - returns error trace when all attempts fail`() = runTest {
        val trace = tri.ai.core.CompletionBuilder()
            .text("list things")
            .executeMultiAttemptJsonList(errorChat(), attempts = 2)

        assertFalse(trace.exec.succeeded())
        assertEquals(2, trace.exec.attempts)
        assertEquals("simulated error", trace.exec.error)
    }

    @Test
    fun `executeMultiAttemptJsonList - returns error when no valid JSON list produced`() = runTest {
        val chat = fakeChat("not json", "also not json")
        val trace = tri.ai.core.CompletionBuilder()
            .text("list things")
            .executeMultiAttemptJsonList(chat, attempts = 2)

        assertFalse(trace.exec.succeeded())
        assertEquals(2, trace.exec.attempts)
        assertNotNull(trace.exec.error)
    }

    @Test
    fun `executeMultiAttemptJsonList - UNION strategy`() = runTest {
        val chat = fakeChat("""["a", "b"]""", """["b", "c"]""")
        val trace = tri.ai.core.CompletionBuilder()
            .text("list things")
            .executeMultiAttemptJsonList(chat, attempts = 2, mergeStrategy = JsonListMergeStrategy.UNION)

        assertTrue(trace.exec.succeeded())
        val result = trace.values?.firstOrNull()?.other as? List<*>
        assertEquals(listOf("a", "b", "c"), result)
    }

    @Test
    fun `executeMultiAttemptJsonList - INTERSECTION strategy`() = runTest {
        val chat = fakeChat("""["a", "b", "c"]""", """["b", "c", "d"]""")
        val trace = tri.ai.core.CompletionBuilder()
            .text("list things")
            .executeMultiAttemptJsonList(chat, attempts = 2, mergeStrategy = JsonListMergeStrategy.INTERSECTION)

        assertTrue(trace.exec.succeeded())
        val result = trace.values?.firstOrNull()?.other as? List<*>
        assertEquals(listOf("b", "c"), result)
    }

    @Test
    fun `executeMultiAttemptJsonList - jsonKey extracts correct field from object responses`() = runTest {
        // Simulates ListGeneratorView responses where known_items appears before items_in_input
        val response = """{"item_category":"animals","known_items":["fox"],"items_in_input":["fox","dog"]}"""
        val chat = fakeChat(response, response)
        val trace = tri.ai.core.CompletionBuilder()
            .text("list animals")
            .executeMultiAttemptJsonList(
                chat, attempts = 2,
                mergeStrategy = JsonListMergeStrategy.UNION,
                jsonKey = "items_in_input"
            )

        assertTrue(trace.exec.succeeded())
        val result = trace.values?.firstOrNull()?.other as? List<*>
        // Should contain fox and dog, NOT just fox (from known_items)
        assertEquals(listOf("fox", "dog"), result)
    }
}
