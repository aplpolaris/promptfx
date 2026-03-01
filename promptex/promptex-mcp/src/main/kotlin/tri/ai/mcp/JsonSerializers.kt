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
package tri.ai.mcp

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import tri.util.json.jsonMapper

/**
 * Handles JSON serialization and deserialization for JSON-RPC messages.
 * Centralizes all JSON processing concerns.
 */
object JsonSerializers {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
        prettyPrint = false
    }

    /** Parse a JSON-RPC request from string. */
    fun parseRequest(line: String): JsonObject =
        json.parseToJsonElement(line).jsonObject

    /** Serialize a JsonElement to string. */
    fun serialize(element: JsonElement): String =
        json.encodeToString(element)

    /** Convert any DTO to JsonElement via Jackson -> string -> kotlinx JsonElement */
    fun toJsonNode(obj: Any): JsonNode =
        jsonMapper.readTree(jsonMapper.writeValueAsString(obj))

    /** Convert a JsonObject like {"k":"v"} into Map<String,String> */
    fun toStringMap(jsonObject: JsonObject): Map<String, String> =
        jsonObject.entries.associate { (k, v) -> k to v.jsonPrimitive.content }

    /** Convert a general object to a kotlinx JsonElement */
    fun Any.toJsonElement(): JsonElement =
        json.parseToJsonElement(jsonMapper.writeValueAsString(this))

    /** Convert a kotlinx JsonElement to Jackson JsonNode */
    fun JsonElement.toJsonNode(): JsonNode =
        jsonMapper.readTree(json.encodeToString(this))
}
