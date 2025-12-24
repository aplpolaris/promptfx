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
package tri.util.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Create a simple JSON object with a single string field. */
fun createObject(key: String, value: String?): ObjectNode =
    jsonMapper.createObjectNode().put(key, value)

/** Create a simple JSON object with a single integer field. */
fun createObject(key: String, value: Int): ObjectNode =
    jsonMapper.createObjectNode().put(key, value)

/** Create a simple JSON object with multiple string fields. */
fun createObject(vararg vars: Pair<String, String>): ObjectNode = jsonMapper.createObjectNode().apply {
    vars.forEach { put(it.first, it.second) }
}

/** Try to parse a string as JSON, returning null if it fails. */
fun String.tryJson(): JsonNode? = try {
    val jsonObject = Json.Default.parseToJsonElement(this) as? JsonObject
    // Convert kotlinx JsonObject to Jackson JsonNode
    jsonObject?.let {
        val jsonString = it.toString()
        jsonMapper.readTree(jsonString)
    }
} catch (x: SerializationException) {
    null
} catch (x: Exception) {
    null
}

/** Convert a Jackson [JsonNode] to a kotlinx.serialization [JsonObject]. */
fun convertToKotlinxJsonObject(input: JsonNode): JsonObject {
    val jsonString = jsonMapper.writeValueAsString(input)
    return Json.parseToJsonElement(jsonString) as JsonObject
}
