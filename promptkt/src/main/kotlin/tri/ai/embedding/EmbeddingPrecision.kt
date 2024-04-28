package tri.ai.embedding

import kotlin.math.pow

/** Precision to use when storing embeddings. */
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