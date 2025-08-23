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
package tri.ai.gemini

import tri.ai.core.EmbeddingModel
import tri.ai.gemini.GeminiModelIndex.EMBED1

/** An embedding service that uses the Gemini API. */
class GeminiEmbeddingModel(override val modelId: String = EMBED1, val client: GeminiClient = GeminiClient.INSTANCE) :
    EmbeddingModel {

    override fun toString() = "$modelId (Gemini)"

    private val embeddingCache = mutableMapOf<Pair<String, Int?>, List<Float>>()

    override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?, progressCallback: ((Int, Int) -> Unit)?): List<List<Double>> {
        val uncached = text.filter { (it to outputDimensionality) !in embeddingCache }
        var processedCount = 0
        val uncachedCalc = uncached.chunked(MAX_EMBEDDING_BATCH_SIZE).flatMap { batch ->
            val result = client.batchEmbedContents(batch, modelId, outputDimensionality).embeddings
            processedCount += batch.size
            progressCallback?.invoke(text.size - uncached.size + processedCount, text.size)
            result
        }
        uncachedCalc.forEachIndexed { index, embedding -> embeddingCache[uncached[index] to outputDimensionality] = embedding.values }
        return text.map { embeddingCache[it to outputDimensionality]!!.map { it.toDouble() } }
    }

    companion object {
        private const val MAX_EMBEDDING_BATCH_SIZE = 100
    }

}
