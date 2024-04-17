package tri.ai.text.chunks.process

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

import kotlinx.coroutines.runBlocking
import tri.ai.embedding.EmbeddingService
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import kotlin.math.pow

/**
 * Utilities for adding embedding information to [TextDoc]s.
 * Enforces a maximum number of embeddings to calculate in one query.
 */
object TextDocEmbeddings {

    /** Max number of embeddings to calculate in one query. */
    const val MAX_EMBEDDING_BATCH_SIZE = 20

    /** Calculate an embedding for a single chunk. */
    suspend fun EmbeddingService.calculate(doc: TextDoc, chunk: TextChunk) =
        calculateEmbedding(chunk.text(doc.all))

    /** Calculate embeddings for a single document. */
    suspend fun EmbeddingService.calculate(doc: TextDoc): List<List<Double>> =
        doc.chunks.map { it.text(doc.all) }.chunked(MAX_EMBEDDING_BATCH_SIZE)
            .flatMap { calculateEmbedding(it) }

    /** Add embedding info for all chunks in a document. */
    fun EmbeddingService.addEmbeddingInfo(doc: TextDoc) {
        val embeddings = runBlocking { calculate(doc) }
        doc.chunks.forEachIndexed { i, chunk ->
            chunk.putEmbeddingInfo(modelId, embeddings[i], precision)
        }
    }

    /** Save embedding info with a chunk. */
    fun TextChunk.putEmbeddingInfo(modelId: String, embedding: List<Double>, precision: EmbeddingPrecision) {
        attributes.putIfAbsent("embeddings", mutableMapOf<String, List<Double>>())
        (attributes["embeddings"] as EmbeddingInfo)[modelId] = embedding.map { precision.op(it) }
    }

    /** Get embedding info. */
    fun TextChunk.getEmbeddingInfo(modelId: String): List<Double>? =
        (attributes["embeddings"] as? EmbeddingInfo)?.get(modelId)

    /** Calculates embedding info for all chunks in a document where it is missing. */
    suspend fun TextDoc.calculateMissingEmbeddings(embeddingService: EmbeddingService) {
        val id = embeddingService.modelId
        val chunksToCalculate = chunks.filter { it.getEmbeddingInfo(id) == null }
        if (chunksToCalculate.isNotEmpty()) {
            chunksToCalculate.chunked(MAX_EMBEDDING_BATCH_SIZE).forEach { batch ->
                embeddingService.calculateEmbedding(batch.map { it.text(all) }).forEachIndexed { i, embedding ->
                    batch[i].putEmbeddingInfo(id, embedding, embeddingService.precision)
                }
            }
        }
    }

}

typealias EmbeddingInfo = MutableMap<String, List<Double>>

enum class EmbeddingPrecision {
    FULL {
        override fun op(x: Double) = x
    },
    FIRST_FOUR {
        override fun op(x: Double) = Math.round(x * 10.0.pow(4.0)) / 10.0.pow(4.0)
    },
    FIRST_EIGHT {
        override fun op(x: Double) = Math.round(x * 10.0.pow(8.0)) / 10.0.pow(8.0)
    };

    /** Apply given transformation to an embedding value, e.g. to reduce storage requirements. */
    abstract fun op(x: Double): Double
}
