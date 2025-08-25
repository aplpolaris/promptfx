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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.tool.wf.MAPPER

/** Validates a [PPlan] object. */
object PPlanValidator {

    /** Checks there are no duplicate saveAs names. */
    fun validateNames(plan: PPlan) {
        val dups = plan.steps.mapNotNull { it.saveAs }.groupBy { it }.filter { it.value.size > 1 }
        require(dups.isEmpty()) { "Duplicate saveAs vars: ${dups.keys}" }
    }

    /** Checks that all tools exist within the registry */
    fun validateToolsExist(plan: PPlan, registry: PExecutableRegistry) {
        val missing = plan.steps.map { it.tool }.filter { registry.get(it) == null }
        require(missing.isEmpty()) {
            "Unknown tools: $missing" +
            "    Valid tools: ${registry.list().map { it.name }}"
        }
    }
    
    /** Validates input schemas for plan steps against their executables' expected schemas. */
    fun validateSchemas(plan: PPlan, registry: PExecutableRegistry) {
        plan.steps.forEach { step ->
            val executable = registry.get(step.tool)
            executable?.inputSchema?.let { schema ->
                val validationErrors = validateJsonAgainstSchema(step.input, schema)
                if (validationErrors.isNotEmpty()) {
                    throw IllegalArgumentException(
                        "Step '${step.tool}' input validation failed: ${validationErrors.joinToString(", ")}"
                    )
                }
            }
        }
    }

    /**
     * Simple JSON schema validation. 
     * Returns a list of validation error messages, or empty list if valid.
     * This is a basic implementation focused on the schema structures we generate.
     */
    private fun validateJsonAgainstSchema(data: JsonNode, schema: JsonNode): List<String> {
        val errors = mutableListOf<String>()
        
        try {
            validateJsonNode(data, schema, "", errors)
        } catch (e: Exception) {
            errors.add("Schema validation error: ${e.message}")
        }
        
        return errors
    }
    
    private fun validateJsonNode(data: JsonNode, schema: JsonNode, path: String, errors: MutableList<String>) {
        // Handle anyOf schemas (for flexible input types)
        if (schema.has("anyOf")) {
            val anyOfArray = schema.get("anyOf")
            var anyValid = false
            for (subSchema in anyOfArray) {
                if (validateJsonAgainstSchema(data, subSchema).isEmpty()) {
                    anyValid = true
                    break
                }
            }
            if (!anyValid) {
                errors.add("$path: Value does not match any allowed schema variant")
            }
            return
        }
        
        // Check type
        schema.get("type")?.let { typeNode ->
            val expectedType = typeNode.asText()
            val actualType = getJsonNodeType(data)
            
            if (actualType != expectedType) {
                errors.add("$path: Expected type '$expectedType' but got '$actualType'")
                return
            }
        }
        
        // For objects, validate properties
        if (data.isObject && schema.has("properties")) {
            val properties = schema.get("properties")
            val required = schema.get("required")?.let { req ->
                (0 until req.size()).map { req.get(it).asText() }.toSet()
            } ?: emptySet()
            
            // Check required properties
            required.forEach { requiredProp ->
                if (!data.has(requiredProp)) {
                    errors.add("$path: Missing required property '$requiredProp'")
                }
            }
            
            // Validate existing properties
            data.fieldNames().forEach { fieldName ->
                if (properties.has(fieldName)) {
                    val fieldPath = if (path.isEmpty()) fieldName else "$path.$fieldName"
                    validateJsonNode(data.get(fieldName), properties.get(fieldName), fieldPath, errors)
                }
            }
        }
        
        // For enums, check allowed values
        if (schema.has("enum")) {
            val enumValues = schema.get("enum")
            val dataValue = data.asText()
            var found = false
            for (enumValue in enumValues) {
                if (enumValue.asText() == dataValue) {
                    found = true
                    break
                }
            }
            if (!found) {
                errors.add("$path: Value '$dataValue' is not in allowed enum values")
            }
        }
    }
    
    private fun getJsonNodeType(node: JsonNode): String = when {
        node.isTextual -> "string"
        node.isInt -> "integer"
        node.isDouble || node.isFloat || node.isBigDecimal -> "number"
        node.isBoolean -> "boolean"
        node.isObject -> "object"
        node.isArray -> "array"
        node.isNull -> "null"
        else -> "unknown"
    }

}
