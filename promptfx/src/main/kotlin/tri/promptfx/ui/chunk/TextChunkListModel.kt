/*-
 * #%L
 * tri.promptfx:promptfx
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
            Pair(ch, doc).asTextChunkViewModel(controller.embeddingStrategy.value!!.strategy.modelId)
        }
        chunkList.setAll(newChunks)
    }

    fun applyEmbeddingFilter(text: String) {
        require(text.isNotBlank()) { "Filter text must not be blank" }
        filterModel.createSemanticFilter(text, chunkList, controller.embeddingStrategy.value!!.strategy.model)
    }

}
