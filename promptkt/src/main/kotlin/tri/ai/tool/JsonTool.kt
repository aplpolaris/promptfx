package tri.ai.tool

import com.aallam.openai.api.chat.Parameters
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

/** A tool that has an explicit JSON schema description. */
abstract class JsonTool(
    val name: String,
    val description: String,
    val jsonSchema: String
) {
    abstract suspend fun run(input: JsonObject): String

    fun jsonSchemaAsParameters() = try {
        Parameters.fromJsonString(jsonSchema)
    } catch (x: SerializationException) {
        throw RuntimeException("Invalid JSON schema: $jsonSchema", x)
    }

}