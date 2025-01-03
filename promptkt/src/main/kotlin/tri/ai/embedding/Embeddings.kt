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

import kotlin.math.pow

/** Compute cosine similarity of two vectors. */
fun cosineSimilarity(resp1: List<Double>, resp2: List<Double>): Double {
    val dotProduct = resp1.zip(resp2).sumOf { it.first * it.second }
    val magnitude1 = resp1.sumOf { it * it }.pow(0.5)
    val magnitude2 = resp2.sumOf { it * it }.pow(0.5)
    return dotProduct / (magnitude1 * magnitude2)
}

/** Compute dot product of two vectors. */
fun List<Float>.dot(resp2: List<Float>) =
    zip(resp2).sumOf { it.first * it.second.toDouble() }
