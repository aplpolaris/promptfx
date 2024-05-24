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
package tri.ai.gemini

import tri.ai.embedding.EmbeddingService
import tri.ai.gemini.GeminiModels.EMBED1
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.process.SmartTextChunker

/** An embedding service that uses the OpenAI API. */
class GeminiEmbeddingService(override val modelId: String = EMBED1, val client: GeminiClient = GeminiClient.INSTANCE) : EmbeddingService {

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

    override fun chunkTextBySections(text: String, maxChunkSize: Int) =
        with (SmartTextChunker(maxChunkSize)) {
            TextChunkRaw(text).chunkBySections(combineShortSections = true)
        }

    companion object {
        private const val MAX_EMBEDDING_BATCH_SIZE = 100
    }

}
