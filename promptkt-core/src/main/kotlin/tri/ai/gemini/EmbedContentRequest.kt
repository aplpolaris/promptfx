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

import kotlinx.serialization.Serializable

@Serializable
data class EmbedContentRequest(
    val content: Content,
    val taskType: EmbeddingTaskType? = null,
    val model: String? = null,
    val title: String? = null,
    val outputDimensionality: Int? = null
)

@Serializable
enum class EmbeddingTaskType {
    TASK_TYPE_UNSPECIFIED,
    RETRIEVAL_QUERY,
    RETRIEVAL_DOCUMENT,
    SEMANTIC_SIMILARITY,
    CLASSIFICATION,
    CLUSTERING,
    QUESTION_ANSWERING,
    FACT_VERIFICATION
}

@Serializable
data class EmbedContentResponse(
    val embedding: ContentEmbedding
)

@Serializable
data class BatchEmbedContentRequest(
    val requests: List<EmbedContentRequest>
)

@Serializable
data class BatchEmbedContentsResponse(
    val embeddings: List<ContentEmbedding>
)

@Serializable
data class ContentEmbedding(
    val values: List<Float>
)
