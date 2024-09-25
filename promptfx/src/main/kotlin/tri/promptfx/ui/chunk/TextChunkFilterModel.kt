/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.ui.chunk

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ToggleButton
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextCompletion
import tri.ai.embedding.EmbeddingService
import tri.ai.embedding.cosineSimilarity
import tri.ai.prompt.AiPrompt
import tri.promptfx.tools.PromptScriptView
import java.util.regex.PatternSyntaxException
import kotlin.error

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
    fun disableFilter(chunkList: List<TextChunkViewModel>) {
        updateFilter(TextFilterType.NONE, "", chunkList, null)
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
        if (text.isBlank()) {
            resetChunkScores(chunkList)
            filter.set { true }
        } else when (filterType.value) {
            TextFilterType.NONE -> {
                resetChunkScores(chunkList)
                filter.set { true }
            }
            TextFilterType.SEARCH -> {
                resetChunkScores(chunkList)
                filter.set { text.lowercase() in it.text.lowercase() }
            }
            TextFilterType.REGEX -> {
                resetChunkScores(chunkList)
                try {
                    val regex = text.toRegex(RegexOption.IGNORE_CASE)
                    filter.set { regex.find(it.text) != null }
                } catch (x: PatternSyntaxException) {
                    tri.util.warning<PromptScriptView>("Invalid regex: ${x.message}")
                    filter.set { false }
                }
            }
            TextFilterType.EMBEDDING -> {
                updateChunkScores(text, chunkList, embeddingService!!) {
                    // generate predicate after score calculation, which takes place in background due to API calls
                    val predicate = predicateFromTopScores(chunkList)
                    filter.set { predicate(it) }
                }
            }
            TextFilterType.PROMPT -> {
                error("Not supported yet")
            }
            else -> error("Unexpected filter")
        }
        isFilterUnapplied.set(false)
    }

    /** Resets chunk scores, setting all to null. */
    private fun resetChunkScores(chunkList: List<TextChunkViewModel>) {
        chunkList.forEach { it.score = null }
    }


    /** Updates scores of chunks using an embedding cosine similarity. */
    private fun updateChunkScores(text: String, chunkList: List<TextChunkViewModel>, model: EmbeddingService, onComplete: () -> Unit) {
        var vector: List<Double>?
        val chunkVectors = mutableMapOf<TextChunkViewModel, Pair<List<Double>?, Float>>()
        runBlocking {
            vector = model.calculateEmbedding(text)
            chunkList.forEach {
                val embed = it.embedding ?: model.calculateEmbedding(it.text)
                val score = cosineSimilarity(vector!!, embed).toFloat()
                chunkVectors[it] = Pair(embed, score)
            }
        }
        runLater {
            chunkVectors.forEach { (chunk, pair) ->
                chunk.embedding = pair.first
                chunk.score = pair.second
            }
            onComplete()
        }
    }

    /** Gets embedding vector of user text for ranking. */
    private fun predicateFromTopScores(collection: List<TextChunkViewModel>): (TextChunkViewModel) -> Boolean {
        val scores = collection.mapNotNull { it.score }.sortedDescending()
        val topScore = scores.first()
        val nthScore = scores.take(EMBEDDING_FILTER_MIN_CHUNKS).last()
        val minScoreToKeep = nthScore - (topScore - nthScore) * EMBEDDING_FILTER_LATITUDE
        return { (it.score ?: -1f) >= minScoreToKeep }
    }

    /**
     * Attempt to filter an input based on a given prompt.
     * Returns true if the response contains "yes" (case-insensitive) anywhere.
     */
    private fun llmFilter(completionEngine: TextCompletion, prompt: String, input: String, maxTokens: Int, temp: Double): Boolean {
        val result = runBlocking {
            AiPrompt(prompt).fill(AiPrompt.INPUT to input)
                .let { completionEngine.complete(it, tokens = maxTokens, temperature = temp) }
                .firstValue
        }
        return result.contains("yes", ignoreCase = true)
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
