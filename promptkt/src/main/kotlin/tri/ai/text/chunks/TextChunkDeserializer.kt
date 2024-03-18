package tri.ai.text.chunks

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/** Deserializes a [TextChunk] from JSON, determining type based on which fields are present. */
class TextChunkDeserializer : JsonDeserializer<TextChunk>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TextChunk {
        val node: JsonNode = p.codec.readTree(p)
        val objectMapper = (p.codec as ObjectMapper).copy().registerKotlinModule()

        return if (node.has("text") && !node.has("first") && !node.has("last")) {
            objectMapper.treeToValue(node, TextChunkRaw::class.java)
        } else if (node.has("first") && node.has("last") && !node.has("text")) {
            objectMapper.treeToValue(node, TextChunkInBook::class.java)
        } else {
            throw IllegalArgumentException("Unknown type of TextChunk")
        }
    }
}