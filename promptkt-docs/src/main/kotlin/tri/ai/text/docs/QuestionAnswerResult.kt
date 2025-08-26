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
package tri.ai.text.docs

import tri.ai.core.TextChatMessage
import tri.ai.embedding.EmbeddingMatch
import tri.ai.embedding.SemanticTextQuery
import tri.ai.embedding.cosineSimilarity
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Result object. */
data class QuestionAnswerResult(
    val query: SemanticTextQuery,
    val matches: List<EmbeddingMatch>,
    val trace: AiPromptTrace,
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
