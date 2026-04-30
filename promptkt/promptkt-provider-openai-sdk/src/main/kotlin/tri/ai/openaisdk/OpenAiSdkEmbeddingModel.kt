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
package tri.ai.openaisdk

import com.openai.models.embeddings.EmbeddingCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tri.ai.core.EmbeddingModel

/** Embedding model using the OpenAI official Java SDK. */
class OpenAiSdkEmbeddingModel(
    override val modelId: String = OpenAiSdkModelIndex.EMBEDDING_ADA,
    override val modelSource: String = OpenAiSdkModelIndex.MODEL_SOURCE,
    val client: OpenAiSdkClient = OpenAiSdkClient.INSTANCE
) : EmbeddingModel {

    override fun toString() = modelDisplayName()

    private val embeddingCache = mutableMapOf<Pair<String, Int?>, List<Double>>()

    override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?): List<List<Double>> {
        val uncached = text.filter { (it to outputDimensionality) !in embeddingCache }
        if (uncached.isNotEmpty()) {
            val computed = uncached.chunked(MAX_EMBEDDING_BATCH_SIZE).flatMap { batch ->
                fetchEmbeddings(batch, outputDimensionality)
            }
            computed.forEachIndexed { index, embedding ->
                embeddingCache[uncached[index] to outputDimensionality] = embedding
            }
        }
        return text.map { embeddingCache[it to outputDimensionality]!! }
    }

    private suspend fun fetchEmbeddings(texts: List<String>, outputDimensionality: Int?): List<List<Double>> =
        withContext(Dispatchers.IO) {
            val paramsBuilder = EmbeddingCreateParams.builder()
                .model(modelId)
                .inputOfArrayOfStrings(texts)
            outputDimensionality?.let { paramsBuilder.dimensions(it.toLong()) }

            val response = client.getClient().embeddings().create(paramsBuilder.build())
            response.data().map { embedding ->
                embedding.embeddingValue().asFloats().map { it.toDouble() }
            }
        }

    companion object {
        private const val MAX_EMBEDDING_BATCH_SIZE = 100
    }

}
