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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.MChatRole
import tri.ai.core.TextChatMessage
import tri.util.json.jsonMapper

class AiOutputTest {

    //region SEALED SUBTYPES

    @Test
    fun testSubtypeContentAccessors() {
        val textOut = AiOutput.Text("hello world")
        assertEquals("hello world", textOut.textContent())
        assertNull(textOut.imageContent())
        assertEquals("hello world", textOut.content())
        assertEquals("hello world", textOut.toString())

        val msg = TextChatMessage(MChatRole.Assistant, "response text")
        val chatOut = AiOutput.ChatMessage(msg)
        assertEquals("response text", chatOut.textContent())
        assertNull(chatOut.imageContent())
        assertEquals(msg, chatOut.content())

        val list = listOf(1, 2, 3)
        val otherOut = AiOutput.Other(list)
        assertEquals("[1, 2, 3]", otherOut.textContent())
        assertNull(otherOut.imageContent())
        assertEquals(list, otherOut.content())
    }

    @Test
    fun testSealedWhenExhaustive() {
        val outputs: List<AiOutput> = listOf(
            AiOutput.Text("hello"),
            AiOutput.ChatMessage(TextChatMessage(MChatRole.User, "hi")),
            AiOutput.Other(42)
        )
        val types = outputs.map { output ->
            when (output) {
                is AiOutput.Text -> "text"
                is AiOutput.ChatMessage -> "message"
                is AiOutput.MultimodalMessage -> "multimodal"
                is AiOutput.Other -> "other"
            }
        }
        assertEquals(listOf("text", "message", "other"), types)
    }

    //endregion

    //region DIRECT SUBTYPE CONSTRUCTION

    @Test
    fun testDirectTextConstruction() {
        val output = AiOutput.Text("text content")
        assertInstanceOf(AiOutput.Text::class.java, output)
        assertEquals("text content", output.text)
    }

    @Test
    fun testDirectMessageConstruction() {
        val msg = TextChatMessage(MChatRole.Assistant, "msg")
        val output = AiOutput.ChatMessage(msg)
        assertInstanceOf(AiOutput.ChatMessage::class.java, output)
        assertEquals(msg, output.message)
    }

    @Test
    fun testDirectOtherConstruction() {
        val data = mapOf("key" to "value")
        val output = AiOutput.Other(data)
        assertInstanceOf(AiOutput.Other::class.java, output)
        assertEquals(data, output.other)
    }

    //endregion

    //region SERIALIZATION

    @Test
    fun testTextSerialization() {
        val output = AiOutput.Text("hello world")
        val json = jsonMapper.writeValueAsString(output)
        assertTrue(json.contains("\"type\":\"text\""))
        assertTrue(json.contains("\"text\":\"hello world\""))
    }

    @Test
    fun testChatMessageSerialization() {
        val msg = TextChatMessage(MChatRole.Assistant, "response")
        val output = AiOutput.ChatMessage(msg)
        val json = jsonMapper.writeValueAsString(output)
        assertTrue(json.contains("\"type\":\"message\""))
    }

    @Test
    fun testOtherSerializationOmitsValue() {
        val output = AiOutput.Other(listOf(1.0, 2.0, 3.0))
        val json = jsonMapper.writeValueAsString(output)
        assertTrue(json.contains("\"type\":\"other\""))
        // The 'other' value is @JsonIgnore - not serialized
        assertFalse(json.contains("1.0"))
    }

    @Test
    fun testTextDeserialization() {
        val json = """{"type":"text","text":"deserialized content"}"""
        val output = jsonMapper.readValue(json, AiOutput::class.java)
        assertInstanceOf(AiOutput.Text::class.java, output)
        assertEquals("deserialized content", (output as AiOutput.Text).text)
    }

    @Test
    fun testChatMessageDeserialization() {
        val json = """{"type":"message","message":{"role":"Assistant","content":"chat response"}}"""
        val output = jsonMapper.readValue(json, AiOutput::class.java)
        assertInstanceOf(AiOutput.ChatMessage::class.java, output)
        assertEquals("chat response", (output as AiOutput.ChatMessage).message.content)
    }

    @Test
    fun testLegacyDeserializationDefaultsToText() {
        // Legacy JSON without a "type" field should default to AiOutput.Text
        val legacyJson = """{"text":"legacy content"}"""
        val output = jsonMapper.readValue(legacyJson, AiOutput::class.java)
        assertInstanceOf(AiOutput.Text::class.java, output)
        assertEquals("legacy content", (output as AiOutput.Text).text)
    }

    //endregion

}
