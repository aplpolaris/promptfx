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
package tri.ai.openai.java

import tri.ai.core.EmbeddingModel
import tri.ai.openai.java.OpenAiJavaModelIndex.EMBEDDING_3_SMALL

/** An embedding service that uses the OpenAI official Java SDK. */
class OpenAiJavaEmbeddingModel(
    override val modelId: String = EMBEDDING_3_SMALL,
    val client: OpenAiJavaClient = OpenAiJavaClient.INSTANCE
) : EmbeddingModel {

    override fun toString() = "$modelId (OpenAI Java SDK)"

    private val embeddingCache = mutableMapOf<Pair<String, Int?>, List<Double>>()

    override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?): List<List<Double>> {
        val uncached = text.filter { (it to outputDimensionality) !in embeddingCache }
        
        if (uncached.isNotEmpty()) {
            val paramsBuilder = com.openai.models.embeddings.EmbeddingCreateParams.builder()
                .model(modelId)
                .input(com.openai.models.embeddings.EmbeddingCreateParams.Input.ofStrings(uncached))
            
            outputDimensionality?.let { paramsBuilder.dimensions(it.toLong()) }
            
            val response = client.client.embeddings().create(paramsBuilder.build())
            
            response.data().forEachIndexed { index, embedding ->
                val values = embedding.embedding().map { it.toDouble() }
                embeddingCache[uncached[index] to outputDimensionality] = values
            }
        }
        
        return text.map { embeddingCache[it to outputDimensionality]!! }
    }

}
