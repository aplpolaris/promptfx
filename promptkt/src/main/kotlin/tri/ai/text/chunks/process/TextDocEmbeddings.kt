package tri.ai.text.chunks.process

import kotlinx.coroutines.runBlocking
import tri.ai.embedding.EmbeddingService
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import kotlin.math.pow

/** Utilities for adding embedding information to [TextDoc]s. */
object TextDocEmbeddings {

    /** Calculate an embedding for a single chunk. */
    suspend fun EmbeddingService.calculate(doc: TextDoc, chunk: TextChunk) =
        calculateEmbedding(chunk.text(doc.all))

    /** Calculate embeddings for a single document. */
    suspend fun EmbeddingService.calculate(doc: TextDoc): List<List<Double>> =
        calculateEmbedding(doc.chunks.map { it.text(doc.all) })

    /** Add embedding info for all chunks in a document. */
    fun EmbeddingService.addEmbeddingInfo(doc: TextDoc, precision: EmbeddingPrecision) {
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