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
package tri.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

val MAPPER: ObjectMapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())

val YAML_MAPPER: ObjectMapper = ObjectMapper(YAMLFactory())
    .registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())

//region JsonNode HELPERS

fun createObject(key: String, value: String?): ObjectNode = MAPPER.createObjectNode().put(key, value)
fun createObject(key: String, value: Int): ObjectNode = MAPPER.createObjectNode().put(key, value)
fun createObject(vararg vars: Pair<String, String>): ObjectNode = MAPPER.createObjectNode().apply {
    vars.forEach { put(it.first, it.second) }
}

/** Try to parse a string as JSON, returning null if it fails. */
fun String.tryJson(): JsonNode? = try {
    val jsonObject = Json.parseToJsonElement(this) as? JsonObject
    // Convert kotlinx JsonObject to Jackson JsonNode
    jsonObject?.let {
        val jsonString = it.toString()
        MAPPER.readTree(jsonString)
    }
} catch (x: SerializationException) {
    null
} catch (x: Exception) {
    null
}

fun convertToKotlinxJsonObject(input: JsonNode): JsonObject {
    val jsonString = MAPPER.writeValueAsString(input)
    return Json.parseToJsonElement(jsonString) as JsonObject
}

//endregion

//region FIELD CONVENTIONS

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
    MAPPER.createObjectNode()
        .put(PARAM_RESULT, result)
        .put("isTerminal", isTerminal)

//endregion
