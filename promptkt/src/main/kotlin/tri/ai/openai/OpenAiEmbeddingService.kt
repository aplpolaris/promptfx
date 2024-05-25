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
package tri.ai.openai

import tri.ai.embedding.EmbeddingService
import tri.ai.openai.OpenAiModelIndex.EMBEDDING_ADA
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.process.SmartTextChunker

/** An embedding service that uses the OpenAI API. */
class OpenAiEmbeddingService(override val modelId: String = EMBEDDING_ADA, val client: OpenAiClient = OpenAiClient.INSTANCE) : EmbeddingService {

    override fun toString() = modelId

    private val embeddingCache = mutableMapOf<Pair<String, Int?>, List<Double>>()

    override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?): List<List<Double>> {
        val uncached = text.filter { (it to outputDimensionality) !in embeddingCache }
        val uncachedCalc = uncached.chunked(MAX_EMBEDDING_BATCH_SIZE).flatMap {
            client.quickEmbedding(modelId, outputDimensionality, it).value!!
        }
        uncachedCalc.forEachIndexed { index, embedding -> embeddingCache[uncached[index] to outputDimensionality] = embedding }
        return text.map { embeddingCache[it to outputDimensionality]!! }
    }

    override fun chunkTextBySections(text: String, maxChunkSize: Int) =
        with (SmartTextChunker(maxChunkSize)) {
            TextChunkRaw(text).chunkBySections(combineShortSections = true)
        }

    companion object {
        private const val MAX_EMBEDDING_BATCH_SIZE = 100
    }

}
