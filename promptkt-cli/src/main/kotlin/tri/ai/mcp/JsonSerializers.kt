package tri.ai.mcp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import tri.ai.pips.core.MAPPER

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
    fun toJsonElement(obj: Any): JsonElement =
        json.parseToJsonElement(MAPPER.writeValueAsString(obj))

    /** Convert a JsonObject like {"k":"v"} into Map<String,String> */
    fun toStringMap(jsonObject: JsonObject): Map<String, String> =
        jsonObject.entries.associate { (k, v) -> k to v.jsonPrimitive.content }

    /** Convert a kotlinx JsonElement to Jackson JsonNode */
    fun JsonElement.toJsonNode() =
        MAPPER.readTree(json.encodeToString(this))
}