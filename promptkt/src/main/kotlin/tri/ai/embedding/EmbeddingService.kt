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

import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.process.EmbeddingPrecision
import tri.ai.text.chunks.process.TextDocEmbeddings.putEmbeddingInfo
import tri.util.info
import java.net.URI

/** An interface for chunking text and calculating embeddings. */
interface EmbeddingService {

    val modelId: String

    /**
     * Divide text at reasonable positions. Each chunk should have a length of at most maxChunkSize.
     * The returned map contains the start and end indices of each chunk.
     */
    fun chunkTextBySections(text: String, maxChunkSize: Int): List<TextChunk>

    /** Calculate embedding for multiple texts. */
    suspend fun calculateEmbedding(text: List<String>): List<List<Double>>

    /** Calculate embedding for a single text. */
    suspend fun calculateEmbedding(text: String): List<Double> =
        calculateEmbedding(listOf(text)).first()

    /** Calculate embedding for a single text. */
    suspend fun calculateEmbedding(vararg text: String): List<List<Double>> =
        calculateEmbedding(listOf(*text))

    /** Chunks a text into sections and calculates the embedding for each section. */
    suspend fun chunkedEmbedding(path: URI, text: String, maxChunkSize: Int): TextDoc {
        info<EmbeddingService>("Calculating embedding for $path...")
        val doc = TextDoc(path.toString(), text).apply {
            metadata.path = path
        }
        val chunks = chunkTextBySections(text, maxChunkSize)
        val chunkEmbeddings = chunks.map { it.text(doc.all) }.chunked(5) // break into smaller chunks of text for slower embedding APIs
            .flatMap { calculateEmbedding(it) }
        chunks.zip(chunkEmbeddings).forEach { (chunk, embedding) ->
            chunk.putEmbeddingInfo(modelId, embedding, EmbeddingPrecision.FIRST_EIGHT)
        }
        doc.chunks.addAll(chunks)
        return doc
    }

}
