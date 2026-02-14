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
package tri.ai.mcp.tool

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import tri.ai.mcp.tool.FakeTools.buildSchemaWithOneRequiredParam
import tri.util.json.jsonMapper

class StubToolTest {

    private val MAPPER = jsonMapper

    @Test
    fun testWrite_empty() {
        val example = StubTool(
            name = "stub-tool",
            description = "A stub tool for testing",
            inputSchema = MAPPER.convertValue<JsonNode>(mapOf<String,Any>()),
            outputSchema = MAPPER.convertValue<JsonNode>(mapOf<String,Any>()),
            hardCodedOutput = buildJsonObject { }
        )
        println(MAPPER.writeValueAsString(example))
    }

    @Test
    fun testWrite_basic() {
        val example = StubTool(
            name = "stub-tool",
            description = "A stub tool for testing",
            inputSchema = buildSchemaWithOneRequiredParam("input_text", "The text to analyze."),
            outputSchema = MAPPER.readTree("""
                {
                  "type": "object",
                  "properties": {
                    "input_text": { "type": "string", "description": "The input text that was analyzed." },
                    "sentiment": { 
                      "type": "string", 
                      "description": "The detected sentiment.", 
                      "enum": ["positive", "negative", "neutral"] 
                    },
                    "confidence": { 
                      "type": "number", 
                      "description": "Confidence score between 0 and 1." ,
                      "minimum": 0,
                      "maximum": 1
                    }
                  },
                  "required": ["input_text", "sentiment", "confidence"]
                }
            """.trimIndent()),
            hardCodedOutput = buildJsonObject {
                put("input_text", "I love programming!")
                put("sentiment", "positive")
                put("confidence", 0.95)
            }
        )
        println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(example))
    }

    @Test
    fun testWrite_library() {
        val tools = mapOf("tools" to StarterToolLibrary().tools.filter { it is StubTool })
        println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(tools))
    }

    @Test
    fun testRead_library() {
        val resource = this::class.java.getResource("resources/stub-tools.json")!!
        val loadedTools = MAPPER.readValue<Map<String,List<StubTool>>>(resource)
        println(loadedTools)
        StarterToolLibrary().apply {
            this.tools = loadedTools["tools"]!!
        }
    }

    @Test
    fun testRead_basic_string() {
        val tools = MAPPER.readValue<StubTool>("""
            {
              "name": "stub-tool",
              "description": "A stub tool for testing",
              "version": "1.0.0",
              "inputSchema": {
                "type": "object",
                "properties": {
                  "input_text": {
                    "type": "string",
                    "description": "The text to analyze."
                  }
                },
                "required": [
                  "input_text"
                ]
              },
              "outputSchema": {
                "type": "object",
                "properties": {
                  "input_text": {
                    "type": "string",
                    "description": "The input text that was analyzed."
                  },
                  "sentiment": {
                    "type": "string",
                    "description": "The detected sentiment.",
                    "enum": [
                      "positive",
                      "negative",
                      "neutral"
                    ]
                  },
                  "confidence": {
                    "type": "number",
                    "description": "Confidence score between 0 and 1.",
                    "minimum": 0,
                    "maximum": 1
                  }
                },
                "required": [
                  "input_text",
                  "sentiment",
                  "confidence"
                ]
              },
              "hardCodedOutput" : {
                "input_text" : {
                  "isString" : true,
                  "content" : "I love programming!",
                  "coerceToInlineType${"$"}kotlinx_serialization_json" : null
                },
                "sentiment" : {
                  "isString" : true,
                  "content" : "positive",
                  "coerceToInlineType${"$"}kotlinx_serialization_json" : null
                },
                "confidence" : {
                  "isString" : false,
                  "content" : "0.95",
                  "coerceToInlineType${"$"}kotlinx_serialization_json" : null
                }
              }
            }
        """.trimIndent())
        println(tools)
    }

}
