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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/** Utilities for generating JSON schemas from PromptDef structures. */
object PromptDefSchema {

    private val mapper = ObjectMapper()

    /** 
     * Generate a JSON schema for the input parameters of a PromptDef.
     * The schema is based on the args field of the PromptDef.
     */
    fun generateInputSchema(def: PromptDef): JsonNode {
        val schema = mapper.createObjectNode()
        schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
        schema.put("type", "object")
        
        val properties = mapper.createObjectNode()
        val required = mapper.createArrayNode()
        
        def.args.forEach { arg ->
            val propSchema = createPropertySchema(arg)
            properties.set<ObjectNode>(arg.name, propSchema)
            
            if (arg.required) {
                required.add(arg.name)
            }
        }
        
        schema.set<ObjectNode>("properties", properties)
        if (required.size() > 0) {
            schema.set<ArrayNode>("required", required)
        }
        
        return schema
    }
    
    /**
     * Generate a JSON schema for the standard output of a PromptExecutable.
     * This returns a schema with a "text" field containing the generated text.
     */
    fun generatePromptOutputSchema(): JsonNode {
        val schema = mapper.createObjectNode()
        schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
        schema.put("type", "object")
        
        val properties = mapper.createObjectNode()
        val textProp = mapper.createObjectNode()
        textProp.put("type", "string")
        textProp.put("description", "The generated text from the prompt template")
        properties.set<ObjectNode>("text", textProp)
        
        val required = mapper.createArrayNode()
        required.add("text")
        
        schema.set<ObjectNode>("properties", properties)
        schema.set<ArrayNode>("required", required)
        
        return schema
    }
    
    /**
     * Generate a JSON schema for chat message input.
     * This accepts either a string or an object with message/text fields.
     */
    fun generateChatInputSchema(): JsonNode {
        val schema = mapper.createObjectNode()
        schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
        
        val anyOf = mapper.createArrayNode()
        
        // Allow direct string input
        val stringSchema = mapper.createObjectNode()
        stringSchema.put("type", "string")
        anyOf.add(stringSchema)
        
        // Allow object with message field
        val objectSchema = mapper.createObjectNode()
        objectSchema.put("type", "object")
        val properties = mapper.createObjectNode()
        val messageProp = mapper.createObjectNode()
        messageProp.put("type", "string")
        properties.set<ObjectNode>("message", messageProp)
        objectSchema.set<ObjectNode>("properties", properties)
        anyOf.add(objectSchema)
        
        // Allow object with text field  
        val textObjectSchema = mapper.createObjectNode()
        textObjectSchema.put("type", "object")
        val textProperties = mapper.createObjectNode()
        val textProp = mapper.createObjectNode()
        textProp.put("type", "string")
        textProperties.set<ObjectNode>("text", textProp)
        textObjectSchema.set<ObjectNode>("properties", textProperties)
        anyOf.add(textObjectSchema)
        
        schema.set<ArrayNode>("anyOf", anyOf)
        return schema
    }
    
    /**
     * Generate a JSON schema for chat message output.
     * This returns an object with a "message" field.
     */
    fun generateChatOutputSchema(): JsonNode {
        val schema = mapper.createObjectNode()
        schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
        schema.put("type", "object")
        
        val properties = mapper.createObjectNode()
        val messageProp = mapper.createObjectNode()
        messageProp.put("type", "string")
        messageProp.put("description", "The response message from the chat model")
        properties.set<ObjectNode>("message", messageProp)
        
        val required = mapper.createArrayNode()
        required.add("message")
        
        schema.set<ObjectNode>("properties", properties)
        schema.set<ArrayNode>("required", required)
        
        return schema
    }
    
    private fun createPropertySchema(arg: PromptArgDef): ObjectNode {
        val propSchema = mapper.createObjectNode()
        
        when (arg.type) {
            PromptArgType.string -> propSchema.put("type", "string")
            PromptArgType.integer -> propSchema.put("type", "integer")
            PromptArgType.number -> propSchema.put("type", "number")
            PromptArgType.boolean -> propSchema.put("type", "boolean")
            PromptArgType.json -> {
                // Allow any JSON value
                propSchema.put("type", "object")
            }
            PromptArgType.enumeration -> {
                propSchema.put("type", "string")
                if (arg.allowedValues.isNotEmpty()) {
                    val enumArray = mapper.createArrayNode()
                    arg.allowedValues.forEach { enumArray.add(it) }
                    propSchema.set<ArrayNode>("enum", enumArray)
                }
            }
        }
        
        arg.description?.let { propSchema.put("description", it) }
        arg.defaultValue?.let { propSchema.put("default", it) }
        
        return propSchema
    }
}