/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import javafx.collections.ObservableList
import tornadofx.*
import tri.ai.core.EmbeddingPrecision
import tri.ai.embedding.EmbeddingMatch
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocEmbeddings.getEmbeddingInfo
import tri.ai.text.chunks.TextDocEmbeddings.putEmbeddingInfo

/** View model for document chunks. */
interface TextChunkViewModel {
    var embedding: List<Double>?
    val embeddingsAvailable: List<String>
    var score: Float?
    val browsable: BrowsableSource?
    val text: String
}

/** Convert an observable list of [Pair<TextDoc, TextChunk>] to a list of [TextChunkViewModel]. */
fun ObservableList<Pair<TextChunk, TextDoc>>.sectionViewModel(embeddingModelId: String): ObservableList<out TextChunkViewModel> {
    val result = observableListOf(map { it.asTextChunkViewModel(embeddingModelId) })
    onChange { result.setAll(map { it.asTextChunkViewModel(embeddingModelId) }) }
    return result
}

/** Wrap [Pair<TextDoc, TextChunk>] as a view model. */
fun Pair<TextChunk, TextDoc>.asTextChunkViewModel(embeddingModelId: String?, score: Float? = null) =
    TextChunkViewModelImpl(second, first, embeddingModelId, score)

/** Wrap [TextChunk] as a view model. */
fun TextChunk.asTextChunkViewModel(parentDoc: TextDoc?, embeddingModelId: String?, score: Float? = null) =
    TextChunkViewModelImpl(parentDoc, this, embeddingModelId, score)

/** Wrap [TextChunk] as a view model. */
class TextChunkViewModelImpl(parentDoc: TextDoc?, val chunk: TextChunk, val embeddingModelId: String?, override var score: Float? = null) :
    TextChunkViewModel {
    constructor(text: String) : this(null, TextChunkRaw(text), null)

    override val browsable = parentDoc?.browsable()
    override var embedding: List<Double>?
        get() = chunk.getEmbeddingInfo(embeddingModelId ?: "")
        set(value) {
            if (value != null) chunk.putEmbeddingInfo(embeddingModelId ?: "", value, EmbeddingPrecision.FULL)
        }
    override val embeddingsAvailable
        get() = chunk.getEmbeddingInfo()?.keys?.toList() ?: emptyList()
    override val text = chunk.text(parentDoc?.all)
}

/** Convert an observable list of [EmbeddingMatch] to a list of [TextChunkViewModel]. */
internal fun ObservableList<EmbeddingMatch>.matchViewModel(): ObservableList<TextChunkViewModel> {
    val result = observableListOf(map { it.asTextChunkViewModel()})
    onChange { result.setAll(map { it.asTextChunkViewModel() }) }
    return result
}

/** Wrap [EmbeddingMatch] as a view model. */
internal fun EmbeddingMatch.asTextChunkViewModel() = object : TextChunkViewModel {
    override var score: Float? = this@asTextChunkViewModel.queryScore
    override var embedding: List<Double>? = chunkEmbedding
    override val embeddingsAvailable = listOf(embeddingModel)
    override val browsable = document.browsable()
    override val text = chunkText
}
