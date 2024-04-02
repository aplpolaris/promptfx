/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import tri.ai.text.chunks.TextDoc

/** A scored match for a semantic query. */
class EmbeddingMatch(
    val query: SemanticTextQuery,
    val document: TextDoc,
    val chunk: TextChunk,
    val chunkEmbedding: List<Double>,
    val score: Double
) {
    @get:JsonIgnore
    val chunkText: String
        get() = chunk.text(document.all)

    @get:JsonIgnore
    val chunkSize: Int
        get() = chunkText.length

    @get:JsonIgnore
    val shortDocName: String
        get() = document.browsable()?.shortNameWithoutExtension ?: document.metadata.id

    override fun toString() = "EmbeddingMatch(document=$shortDocName, chunk size=${chunk.text(document.all).length}, score=$score)"

}