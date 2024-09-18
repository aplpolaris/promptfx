package tri.promptfx.ui.chunk

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ToggleButton
import kotlinx.coroutines.runBlocking
import tornadofx.Component
import tornadofx.ScopedInstance
import tornadofx.onChange
import tri.ai.embedding.EmbeddingService
import tri.ai.embedding.cosineSimilarity
import tri.promptfx.tools.PromptScriptView
import java.util.regex.PatternSyntaxException

/** Component for filtering text chunks. */
class TextChunkFilterModel : Component(), ScopedInstance {

    /** Text for creating filter. */
    val filterText = SimpleStringProperty("")
    /** Type for filter. */
    val filterType = SimpleObjectProperty(TextFilterType.NONE)
    /** Flag indicating filter has changed. */
    val isFilterUnapplied = SimpleBooleanProperty(false)
    /** Model for filtering. */
    val filter = SimpleObjectProperty<(TextChunkViewModel) -> Boolean> { true }

    init {
        filterText.onChange { isFilterUnapplied.set(true) }
        filterType.onChange { isFilterUnapplied.set(true) }
    }

    //region FILTER UPDATERS

    /** Creates a semantic filter based on the given chunk. */
    fun createSemanticFilter(text: String, chunkList: List<TextChunkViewModel>, embeddingService: EmbeddingService) {
        filterText.set(text)
        filterType.set(TextFilterType.EMBEDDING)
        updateFilter(TextFilterType.EMBEDDING, text, chunkList, embeddingService)
    }

    /** Disable filtering. */
    fun disableFilter() {
        updateFilter(TextFilterType.NONE, "", emptyList(), null)
    }

    /**
     * Update filter of given type.
     * @param type type of filter
     * @param filterText text to filter by
     * @param chunkList list of chunks, used when filtering by embeddings to generate minimum match score
     */
    private fun updateFilter(type: TextFilterType, filterText: String, chunkList: List<TextChunkViewModel>, embeddingService: EmbeddingService?) {
        this.filterType.set(type)
        this.filterText.set(filterText)
        updateFilter(chunkList, embeddingService)
    }

    /** Update filter with current text and type. */
    fun updateFilter(chunkList: List<TextChunkViewModel>, embeddingService: EmbeddingService?) {
        val text = filterText.value
        when (filterType.value) {
            TextFilterType.NONE -> {
                filter.set { true }
            }
            TextFilterType.SEARCH -> {
                filter.set { text.lowercase() in it.text.lowercase() }
            }
            TextFilterType.REGEX -> {
                try {
                    val regex = text.toRegex(RegexOption.IGNORE_CASE)
                    filter.set { regex.find(it.text) != null }
                } catch (x: PatternSyntaxException) {
                    tri.util.warning<PromptScriptView>("Invalid regex: ${x.message}")
                    filter.set { false }
                }
            }
            TextFilterType.EMBEDDING -> {
                val predicate = predicateFromTopEmbeddingMatches(text, chunkList, embeddingService!!)
                filter.set { predicate(it) }
            }
            else -> error("Unexpected filter")
        }
        isFilterUnapplied.set(false)
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

    //endregion

    companion object {
        /** Minimum number of chunks to include when filtering by embedding. */
        private const val EMBEDDING_FILTER_MIN_CHUNKS = 20
        /** Top chunks filtering by embedding includes the top N chunks and this times the difference between top/bottom scores. */
        private const val EMBEDDING_FILTER_LATITUDE = 0.25

        /** Bind selection flag to target type. */
        fun ToggleButton.selectWhen(model: TextChunkFilterModel, type: TextFilterType) {
            model.filterType.onChange { isSelected = it == type }
            selectedProperty().onChange { if (isSelected) model.filterType.set(type) }
            isSelected = model.filterType.value == type
        }
    }
}

/** Types of chunk filters. */
enum class TextFilterType {
    NONE,
    SEARCH,
    REGEX,
    WILDCARD,
    EMBEDDING,
    PROMPT,
    FIELD
}