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
