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
package tri.ai.embedding

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunkInDoc
import tri.ai.text.chunks.TextDoc

/** A scored match for a semantic query. */
class EmbeddingMatch(
    @get:JsonIgnore
    val query: SemanticTextQuery,
    @get:JsonIgnore
    val document: TextDoc,
    @get:JsonIgnore
    val chunk: TextChunk,
    val embeddingModel: String,
    val chunkEmbedding: List<Double>,
    val queryScore: Float
) {
    val source: Map<String, Any?>
        get() = mapOf(
            "metadata" to document.metadata,
            "first" to (chunk as? TextChunkInDoc)?.first,
            "last" to (chunk as? TextChunkInDoc)?.last
        )
    val chunkText: String
        get() = chunk.text(document.all)

    @get:JsonIgnore
    val chunkSize: Int
        get() = chunkText.length

    @get:JsonIgnore
    val shortDocName: String
        get() = document.browsable()?.shortNameWithoutExtension ?: document.metadata.id

    var responseScore: Float? = null

    override fun toString() = "EmbeddingMatch(document=$shortDocName, " +
            "chunk size=${chunk.text(document.all).length}, " +
            "score=${"%.3f".format(queryScore)})"

}
