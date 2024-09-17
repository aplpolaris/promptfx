package tri.promptfx.library

import javafx.beans.property.SimpleObjectProperty
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.EmbeddingService
import tri.ai.embedding.cosineSimilarity
import tri.promptfx.PromptFxController
import tri.promptfx.tools.PromptScriptView
import tri.promptfx.ui.TextChunkViewModel
import java.util.regex.PatternSyntaxException

/** Component for filtering text chunks. */
class TextChunkFilterModel : Component(), ScopedInstance {

    /** Global controller. */
    val controller: PromptFxController by inject()

    /** Model for filtering. */
    val model = SimpleObjectProperty<(TextChunkViewModel) -> Boolean> { false }

    /**
     * Update filter of given type.
     * @param type type of filter
     * @param filterText text to filter by
     * @param chunkList list of chunks, used when filtering by embeddings to generate minimum match score
     */
    fun updateFilter(type: FilterType, filterText: String, chunkList: List<TextChunkViewModel>) {
        when (type) {
            FilterType.NONE -> {
                model.set { true }
            }
            FilterType.SEARCH -> {
                model.set { filterText.lowercase() in it.text.lowercase() }
            }
            FilterType.REGEX -> {
                try {
                    val regex = filterText.toRegex(RegexOption.IGNORE_CASE)
                    model.set { regex.find(it.text) != null }
                } catch (x: PatternSyntaxException) {
                    tri.util.warning<PromptScriptView>("Invalid regex: ${x.message}")
                    model.set { false }
                }
            }
            FilterType.EMBEDDING -> {
                val predicate = predicateFromTopEmbeddingMatches(filterText, chunkList, controller.embeddingService.value!!)
                model.set { predicate(it) }
            }
            else -> kotlin.error("Unexpected filter")
        }
    }

    /** Gets embedding vector of user text for ranking. */
    private fun predicateFromTopEmbeddingMatches(text: String, collection: List<TextChunkViewModel>, model: EmbeddingService): (TextChunkViewModel) -> Boolean {
        val vector = runBlocking { model.calculateEmbedding(text) }
        val scores = collection.map {
            cosineSimilarity(vector, it.embedding!!)
        }.sortedDescending()
        val topScore = scores.first()
        val nthScore = scores.take(EMBEDDING_FILTER_MIN_CHUNKS).last()
        val minScoreToKeep = nthScore - (topScore - nthScore) * EMBEDDING_FILTER_LATITUDE
        return { cosineSimilarity(vector, it.embedding!!) >= minScoreToKeep }
    }

    fun disableFilter() {
        updateFilter(FilterType.NONE, "", emptyList())
    }

    companion object {
        /** Minimum number of chunks to include when filtering by embedding. */
        private const val EMBEDDING_FILTER_MIN_CHUNKS = 20
        /** Top chunks filtering by embedding includes the top N chunks and this times the difference between top/bottom scores. */
        private const val EMBEDDING_FILTER_LATITUDE = 0.25
    }
}

/** Types of chunk filters. */
enum class FilterType {
    NONE,
    SEARCH,
    REGEX,
    WILDCARD,
    EMBEDDING,
    PROMPT,
    FIELD
}