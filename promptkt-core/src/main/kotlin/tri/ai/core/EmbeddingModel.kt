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
package tri.ai.core

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * An interface for chunking text and calculating embeddings.
 * Clients are responsible for API limitations, e.g. that limit the number of embeddings that can be calculated in a single API call.
 */
interface EmbeddingModel {

    val precision
        get() = EmbeddingPrecision.FIRST_EIGHT

    val modelId: String

    /** Calculate embedding for multiple texts. */
    suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int? = null): List<List<Double>>

    /** Calculate embedding for a single text. */
    suspend fun calculateEmbedding(text: String, outputDimensionality: Int? = null): List<Double> =
        calculateEmbedding(listOf(text), outputDimensionality).first()

    /** Calculate embedding for a single text. */
    suspend fun calculateEmbedding(vararg text: String, outputDimensionality: Int? = null): List<List<Double>> =
        calculateEmbedding(listOf(*text), outputDimensionality)

}

/** Precision to use when storing embeddings. */
enum class EmbeddingPrecision {
    FULL {
        override fun op(x: Double) = x
    },
    FIRST_FOUR {
        override fun op(x: Double) = (x * 10.0.pow(4.0)).roundToLong() / 10.0.pow(4.0)
    },
    FIRST_EIGHT {
        override fun op(x: Double) = (x * 10.0.pow(8.0)).roundToLong() / 10.0.pow(8.0)
    };

    /** Apply given transformation to an embedding value, e.g. to reduce storage requirements. */
    abstract fun op(x: Double): Double
}

