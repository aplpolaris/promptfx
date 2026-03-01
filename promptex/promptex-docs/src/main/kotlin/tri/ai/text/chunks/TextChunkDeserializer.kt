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
            objectMapper.treeToValue(node, TextChunkInDoc::class.java)
        } else {
            throw IllegalArgumentException("Unknown type of TextChunk")
        }
    }
}
