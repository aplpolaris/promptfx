package tri.ai.text.docs

import tri.ai.embedding.EmbeddingMatch
import tri.ai.embedding.SemanticTextQuery
import tri.ai.embedding.cosineSimilarity
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Result object. */
data class QuestionAnswerResult(
    val query: SemanticTextQuery,
    val matches: List<EmbeddingMatch>,
    val trace: AiPromptTrace<String>,
    val responseEmbeddings: List<List<Double>>
) {
    override fun toString() = trace.output?.outputs?.joinToString() ?: "No response. Question: ${query.query}"

    /** Calculates the similarity between the question and response. */
    val responseScore
        get() = responseEmbeddings.map { cosineSimilarity(query.embedding, it).toFloat() }

    /** Split a QA result into multiple QA results, one per output. */
    fun splitOutputs(): List<QuestionAnswerResult> {
        return trace.values?.map { output ->
            val trace2 = trace.copy()
            trace2.output = AiOutputInfo(listOf(output))
            QuestionAnswerResult(query, matches, trace2, responseEmbeddings)
        } ?: listOf(this)
    }

    /** Formats the result of the QA task. */
    fun formatResult(): FormattedText {
        val result = mutableListOf(FormattedTextNode(trace.values?.joinToString() ?: "No response."))
        val docs = matches.mapNotNull { it.document.browsable() }
            .filter { it.shortNameWithoutExtension.isNotBlank() }
            .toSet()
        docs.sortedByDescending { it.shortNameWithoutExtension.length }.forEach { doc ->
            result.splitOn(doc.shortNameWithoutExtension) {
                FormattedTextNode(doc.shortNameWithoutExtension,
                    hyperlink = doc.file?.absolutePath ?: doc.uri.path)
            }
        }
        result.splitOn("Citations:") { FormattedTextNode(it, BOLD_STYLE) }
        // multiple possible citation formats
        result.splitOn("\\[[0-9]+(,\\s*[0-9]+)*]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        result.splitOn("\\[Citation [0-9]+(,\\s*[0-9]+)*]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        result.splitOn("\\[\\[[0-9]+(,\\s*[0-9]+)*]]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        result.splitOn("\\[\\[Citation [0-9]+(,\\s*[0-9]+)*]]".toRegex()) { FormattedTextNode(it, LINK_STYLE) }
        return FormattedText(result)
    }

    companion object {
        private const val BOLD_STYLE = "-fx-font-weight: bold;"
        private const val LINK_STYLE = "-fx-fill: #8abccf; -fx-font-weight: bold;"
    }
}