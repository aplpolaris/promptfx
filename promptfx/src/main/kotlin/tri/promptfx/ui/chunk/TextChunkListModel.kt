package tri.promptfx.ui.chunk

import tornadofx.Component
import tornadofx.ScopedInstance
import tornadofx.observableListOf
import tornadofx.onChange
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.promptfx.PromptFxController

/** Model for a list of chunks that can be filtered. */
class TextChunkListModel : Component(), ScopedInstance {

    val controller: PromptFxController by inject()
    private val filterModel: TextChunkFilterModel by inject()

    val chunkList = observableListOf<TextChunkViewModel>()
    val filteredChunkList = observableListOf<TextChunkViewModel>()
    val chunkSelection = observableListOf<TextChunkViewModel>()

    init {
        chunkList.onChange { refilter() }
        filterModel.filter.onChange { refilter() }
    }

    /** Refilter chunks by applying the current chunk filter. */
    fun refilter() {
        val filter = filterModel.filter.value!!
        val filtered = chunkList.filter(filter).sortedByDescending { it.score ?: -1f }
        filteredChunkList.setAll(filtered)
    }

    fun setChunkList(chunks: List<Pair<TextChunk, TextDoc>>) {
        val newChunks = chunks.map { (ch, doc) ->
            Pair(ch, doc).asTextChunkViewModel(controller.embeddingService.value.modelId)
        }
        chunkList.setAll(newChunks)
    }

    fun applyEmbeddingFilter(text: String) {
        require(text.isNotBlank()) { "Filter text must not be blank" }
        filterModel.createSemanticFilter(text, chunkList, controller.embeddingService.value)
    }

}