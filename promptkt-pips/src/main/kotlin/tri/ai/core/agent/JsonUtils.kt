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
package tri.ai.core.agent

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

val MAPPER: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
val YAML_MAPPER: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

//region JsonNode HELPERS

fun createObject(key: String, value: String?) = MAPPER.createObjectNode().put(key, value)
fun createObject(key: String, value: Int) = MAPPER.createObjectNode().put(key, value)

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

/** Extracts the most likely text input from a JsonNode, checking common fields. */
val JsonNode.inputText: String
    get() = when {
        isTextual -> asText()
        has("input") -> get("input").asText()
        has("request") -> get("request").asText()
        has("text") -> get("text").asText()
        else -> toString()
    }

/** Creates a standard result JsonNode with "result" and "isTerminal" fields. */
fun createResult(result: String, isTerminal: Boolean = false) =
    MAPPER.createObjectNode()
        .put("result", result)
        .put("isTerminal", isTerminal)

//endregion