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
import tri.ai.embedding.TextChunk
import tri.ai.embedding.TextChunker
import tri.ai.openai.OpenAiModels.EMBEDDING_ADA

/** An embedding service that uses the OpenAI API. */
class OpenAiEmbeddingService(override val modelId: String = EMBEDDING_ADA, val client: OpenAiClient = OpenAiClient.INSTANCE) : EmbeddingService {

    override fun toString() = modelId

    private val embeddingCache = mutableMapOf<String, List<Double>>()

    override suspend fun calculateEmbedding(text: List<String>): List<List<Double>> {
        val res = client.quickEmbedding(inputs = text, modelId = modelId).value
            ?: throw IllegalStateException("No embedding returned.")
        res.forEachIndexed { index, list -> embeddingCache[text[index]] = list }
        return res
    }

    override fun chunkTextBySections(text: String, maxChunkSize: Int) =
        with (TextChunker(maxChunkSize)) {
            TextChunk(text).chunkBySections(combineShortSections = true)
        }

}
