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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.databind.ObjectMapper

class PromptDefSchemaTest {

    @Test
    fun `generates input schema from PromptDef args`() {
        val prompt = PromptDef(
            id = "test/prompt",
            args = listOf(
                PromptArgDef("color", "A color name", true, PromptArgType.string),
                PromptArgDef("count", "Number of items", false, PromptArgType.integer, "5")
            ),
            template = "There are {{count}} {{color}} items."
        )
        
        val schema = PromptDefSchema.generateInputSchema(prompt)
        
        assertTrue(schema.has("\$schema"))
        assertEquals("object", schema.get("type").asText())
        assertTrue(schema.has("properties"))
        assertTrue(schema.has("required"))
        
        val properties = schema.get("properties")
        assertTrue(properties.has("color"))
        assertTrue(properties.has("count"))
        
        val colorProp = properties.get("color")
        assertEquals("string", colorProp.get("type").asText())
        assertEquals("A color name", colorProp.get("description").asText())
        
        val countProp = properties.get("count")
        assertEquals("integer", countProp.get("type").asText())
        assertEquals("5", countProp.get("default").asText())
        
        val required = schema.get("required")
        assertEquals(1, required.size())
        assertEquals("color", required.get(0).asText())
    }

    @Test
    fun `generates schema for enumeration type`() {
        val prompt = PromptDef(
            id = "test/enum-prompt",
            args = listOf(
                PromptArgDef("size", "Size option", true, PromptArgType.enumeration, 
                    allowedValues = listOf("small", "medium", "large"))
            ),
            template = "Size: {{size}}"
        )
        
        val schema = PromptDefSchema.generateInputSchema(prompt)
        val properties = schema.get("properties")
        val sizeProp = properties.get("size")
        
        assertEquals("string", sizeProp.get("type").asText())
        assertTrue(sizeProp.has("enum"))
        
        val enumValues = sizeProp.get("enum")
        assertEquals(3, enumValues.size())
        assertEquals("small", enumValues.get(0).asText())
        assertEquals("medium", enumValues.get(1).asText())
        assertEquals("large", enumValues.get(2).asText())
    }

    @Test
    fun `generates prompt output schema`() {
        val schema = PromptDefSchema.generatePromptOutputSchema()
        
        assertTrue(schema.has("\$schema"))
        assertEquals("object", schema.get("type").asText())
        
        val properties = schema.get("properties")
        assertTrue(properties.has("text"))
        
        val textProp = properties.get("text")
        assertEquals("string", textProp.get("type").asText())
        
        val required = schema.get("required")
        assertEquals(1, required.size())
        assertEquals("text", required.get(0).asText())
    }

    @Test
    fun `generates chat input schema with anyOf`() {
        val schema = PromptDefSchema.generateChatInputSchema()
        
        assertTrue(schema.has("anyOf"))
        val anyOf = schema.get("anyOf")
        assertEquals(3, anyOf.size())
        
        // First option: string
        val stringOption = anyOf.get(0)
        assertEquals("string", stringOption.get("type").asText())
        
        // Second option: object with message
        val messageOption = anyOf.get(1)
        assertEquals("object", messageOption.get("type").asText())
        assertTrue(messageOption.get("properties").has("message"))
        
        // Third option: object with text
        val textOption = anyOf.get(2)
        assertEquals("object", textOption.get("type").asText())
        assertTrue(textOption.get("properties").has("text"))
    }

    @Test
    fun `generates chat output schema`() {
        val schema = PromptDefSchema.generateChatOutputSchema()
        
        assertEquals("object", schema.get("type").asText())
        
        val properties = schema.get("properties")
        assertTrue(properties.has("message"))
        
        val messageProp = properties.get("message")
        assertEquals("string", messageProp.get("type").asText())
        
        val required = schema.get("required")
        assertEquals(1, required.size())
        assertEquals("message", required.get(0).asText())
    }

    @Test
    fun `handles empty args list`() {
        val prompt = PromptDef(id = "test/no-args", template = "Hello world!")
        val schema = PromptDefSchema.generateInputSchema(prompt)
        
        assertEquals("object", schema.get("type").asText())
        val properties = schema.get("properties")
        assertEquals(0, properties.size())
        assertFalse(schema.has("required"))
    }
}