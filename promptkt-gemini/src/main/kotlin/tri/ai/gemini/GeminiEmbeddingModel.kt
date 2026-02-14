/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.gemini

import tri.ai.core.EmbeddingModel
import tri.ai.gemini.GeminiModelIndex.EMBED4

/** An embedding service that uses the Gemini API. */
class GeminiEmbeddingModel(override val modelId: String = EMBED4, val client: GeminiClient = GeminiClient.INSTANCE) :
    EmbeddingModel {

    override fun toString() = "$modelId (Gemini)"

    private val embeddingCache = mutableMapOf<Pair<String, Int?>, List<Float>>()

    override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?): List<List<Double>> {
        val uncached = text.filter { (it to outputDimensionality) !in embeddingCache }
        val uncachedCalc = uncached.chunked(MAX_EMBEDDING_BATCH_SIZE).flatMap {
            client.batchEmbedContents(it, modelId, outputDimensionality).embeddings
        }
        uncachedCalc.forEachIndexed { index, embedding -> embeddingCache[uncached[index] to outputDimensionality] = embedding.values }
        return text.map { embeddingCache[it to outputDimensionality]!!.map { it.toDouble() } }
    }

    companion object {
        private const val MAX_EMBEDDING_BATCH_SIZE = 100
    }

}
