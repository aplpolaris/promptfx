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
package tri.util.json

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonSchemaUtilsTest {

    data class SimpleClass(
        val name: String,
        val age: Int
    )

    data class ComplexClass(
        val title: String,
        val count: Int,
        val active: Boolean,
        val tags: List<String>,
        val map: Map<String, Int>,
        val sub: SimpleClass
    )

    @Test
    fun testGenerateJsonSchemaStringFromJavaClass() {
        val schemaString = generateJsonSchemaString(SimpleClass::class.java)
        println(schemaString)
        assertNotNull(schemaString)
        assertTrue(schemaString.contains("name"))
        assertTrue(schemaString.contains("age"))
        assertTrue(schemaString.contains("properties"))
        
        val parsed = jsonMapper.readTree(schemaString)
        assertTrue(parsed.isObject) { "invalid json" }

        println(generateJsonSchemaString(ComplexClass::class.java))
    }

    @Test
    fun testGenerateJsonSchemaStringFromKotlinClass() {
        val schemaString = generateJsonSchemaString(SimpleClass::class)
        println(schemaString)
        assertNotNull(schemaString)
        assertTrue(schemaString.contains("name"))
        assertTrue(schemaString.contains("age"))
        assertTrue(schemaString.contains("properties"))
        
        val parsed = jsonMapper.readTree(schemaString)
        assertTrue(parsed.isObject) { "invalid json" }
    }

    @Test
    fun testGenerateJsonSchemaForComplexClass() {
        val schema = generateJsonSchema(ComplexClass::class)
        assertNotNull(schema)
        assertTrue(schema.isObject)
        
        val properties = schema.get("properties")
        assertTrue(properties.has("title"))
        assertTrue(properties.has("count"))
        assertTrue(properties.has("active"))
        assertTrue(properties.has("tags"))
        assertTrue(properties.has("map"))
        assertTrue(properties.has("sub"))
    }

    @Test
    fun testReadJsonSchemaValid() {
        val schemaString = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              }
            }
        """.trimIndent()
        
        val schema = readJsonSchema(schemaString)
        assertNotNull(schema)
        assertTrue(schema.isObject)
        assertEquals("object", schema.get("type").asText())
        assertTrue(schema.has("properties"))
    }

    @Test
    fun testReadJsonSchemaInvalidJson() {
        val invalidJson = "{ invalid json }"
        assertThrows<Exception> {
            readJsonSchema(invalidJson)
        }
    }

    @Test
    fun testReadJsonSchemaNotObject() {
        val arrayJson = """["item1", "item2"]"""
        assertThrows<IllegalArgumentException> {
            readJsonSchema(arrayJson)
        }
    }

    @Test
    fun testBuildSchemaWithOneRequiredParam() {
        val schema = buildSchemaWithOneRequiredParam("input_text", "The text to analyze.")
        
        assertNotNull(schema)
        assertTrue(schema.isObject)
        assertEquals("object", schema.get("type").asText())
        
        val properties = schema.get("properties")
        assertTrue(properties.has("input_text"))
        assertEquals("string", properties.get("input_text").get("type").asText())
        assertEquals("The text to analyze.", properties.get("input_text").get("description").asText())
        
        val required = schema.get("required")
        assertNotNull(required)
        assertTrue(required.isArray)
        assertEquals(1, required.size())
        assertEquals("input_text", required.get(0).asText())
    }

    @Test
    fun testBuildSchemaWithOneRequiredParamInteger() {
        val schema = buildSchemaWithOneRequiredParam("count", "The count value.", "integer")
        
        assertNotNull(schema)
        val properties = schema.get("properties")
        assertEquals("integer", properties.get("count").get("type").asText())
        assertEquals("The count value.", properties.get("count").get("description").asText())
    }

    @Test
    fun testBuildSchemaWithOneOptionalParam() {
        val schema = buildSchemaWithOneOptionalParam("optional_field", "An optional field.")
        
        assertNotNull(schema)
        assertTrue(schema.isObject)
        assertEquals("object", schema.get("type").asText())
        
        val properties = schema.get("properties")
        assertTrue(properties.has("optional_field"))
        assertEquals("string", properties.get("optional_field").get("type").asText())
        assertEquals("An optional field.", properties.get("optional_field").get("description").asText())
        
        // Should not have a required array
        assertFalse(schema.has("required"))
    }

    @Test
    fun testBuildSchemaWithOneOptionalParamBoolean() {
        val schema = buildSchemaWithOneOptionalParam("flag", "A boolean flag.", "boolean")
        
        assertNotNull(schema)
        val properties = schema.get("properties")
        assertEquals("boolean", properties.get("flag").get("type").asText())
        assertEquals("A boolean flag.", properties.get("flag").get("description").asText())
    }

    @Test
    fun testCreateJsonSchema() {
        val schema = createJsonSchema(
            "name" to "The person's name",
            "email" to "The person's email address"
        )
        
        assertNotNull(schema)
        assertTrue(schema.isObject)
        assertEquals("object", schema.get("type").asText())
        
        val properties = schema.get("properties")
        assertTrue(properties.has("name"))
        assertTrue(properties.has("email"))
        assertEquals("string", properties.get("name").get("type").asText())
        assertEquals("The person's name", properties.get("name").get("description").asText())
        assertEquals("string", properties.get("email").get("type").asText())
        assertEquals("The person's email address", properties.get("email").get("description").asText())
        
        val required = schema.get("required")
        assertNotNull(required)
        assertTrue(required.isArray)
        assertEquals(2, required.size())
    }
}
