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