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

import tri.ai.text.chunks.TextChunk

/** An interface for an embedding index. */
abstract class EmbeddingIndex(val embeddingService: EmbeddingService) {
    /** Find the most similar section to the query. */
    abstract suspend fun findMostSimilar(query: String, n: Int): List<EmbeddingMatch>
}

/** A no-op version of the embedding index. */
object NoOpEmbeddingIndex : EmbeddingIndex(NoOpEmbeddingService) {
    override suspend fun findMostSimilar(query: String, n: Int) = listOf<EmbeddingMatch>()
}

/** A no-op version of the embedding service. */
object NoOpEmbeddingService : EmbeddingService {
    override val modelId = "NONE"
    override fun chunkTextBySections(text: String, maxChunkSize: Int) = listOf<TextChunk>()
    override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?) = text.map { listOf<Double>(0.0) }
}

