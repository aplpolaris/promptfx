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

import com.fasterxml.jackson.databind.JsonNode

/** Parses a JSON schema string as a [com.fasterxml.jackson.databind.JsonNode]. */
// TODO - validation
fun readJsonSchema(schema: String) =
    jsonMapper.readTree(schema)

/** Creates a JSON schema from a map of field names to descriptions, assuming string fields for each. */
fun createJsonSchema(vararg fields: Pair<String, String>) =
    jsonMapper.createObjectNode().apply {
        put("type", "object")
        val props = putObject("properties")
        fields.forEach { (name, description) ->
            props.putObject(name).apply {
                put("type", "string")
                put("description", description)
            }
        }
        val required = putArray("required")
        fields.forEach { required.add(it.first) }
    }

const val PARAM_INPUT = "input"
const val PARAM_REQUEST = "request"
const val PARAM_TEXT = "text"
const val PARAM_RESULT = "result"

const val STRING_INPUT_SCHEMA = """{"type":"object","properties":{"$PARAM_INPUT":{"type":"string"}}}"""
const val INTEGER_INPUT_SCHEMA = """{"type":"object","properties":{"$PARAM_INPUT":{"type":"integer"}}}"""
const val OUTPUT_SCHEMA = """{"type":"object","properties":{"$PARAM_RESULT":{"type":"string"}}}"""

/** Extracts the most likely text input from a JsonNode, checking common fields. */
val JsonNode.inputText: String
    get() = when {
        isTextual -> asText()
        has(PARAM_INPUT) -> get(PARAM_INPUT).asText()
        has(PARAM_REQUEST) -> get(PARAM_REQUEST).asText()
        has(PARAM_TEXT) -> get(PARAM_TEXT).asText()
        else -> toString()
    }

/** Creates a standard result JsonNode with "result" and "isTerminal" fields. */
fun createResult(result: String, isTerminal: Boolean = false) =
    jsonMapper.createObjectNode()
        .put(PARAM_RESULT, result)
        .put("isTerminal", isTerminal)
