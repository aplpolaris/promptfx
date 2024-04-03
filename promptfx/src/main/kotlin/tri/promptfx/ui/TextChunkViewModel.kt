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
package tri.promptfx.ui

import javafx.collections.ObservableList
import tornadofx.*
import tri.ai.embedding.EmbeddingMatch
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunkRaw
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo

/** View model for document chunks. */
interface TextChunkViewModel {
    val embedding: List<Double>?
    val score: Float?
    val browsable: BrowsableSource?
    val text: String
}

/** Convert an observable list of [EmbeddingMatch] to a list of [TextChunkViewModel]. */
fun ObservableList<EmbeddingMatch>.matchViewModel(): ObservableList<TextChunkViewModel> {
    val result = observableListOf(map { it.asTextChunkViewModel()})
    onChange { result.setAll(map { it.asTextChunkViewModel() }) }
    return result
}

/** Convert an observable list of [Pair<TextDoc, TextChunk>] to a list of [TextChunkViewModel]. */
fun ObservableList<Pair<TextDoc, TextChunk>>.sectionViewModel(embeddingModelId: String): ObservableList<TextChunkViewModel> {
    val result = observableListOf(map { it.asTextChunkViewModel(embeddingModelId) })
    onChange { result.setAll(map { it.asTextChunkViewModel(embeddingModelId) }) }
    return result
}

/** Wrap [EmbeddingMatch] as a view model. */
fun EmbeddingMatch.asTextChunkViewModel() = object : TextChunkViewModel {
    override val score = this@asTextChunkViewModel.queryScore
    override val embedding = chunkEmbedding
    override val browsable = document.browsable()
    override val text = chunkText
}

/** Wrap [Pair<TextDoc, TextChunk>] as a view model. */
fun Pair<TextDoc, TextChunk>.asTextChunkViewModel(embeddingModelId: String) = object : TextChunkViewModel {
    override val score = null
    override val embedding = second.getEmbeddingInfo(embeddingModelId)
    override val browsable = first.browsable()
    override val text = second.text(first.all)
}

/** Wrap [TextChunk] as a view model. */
fun TextChunk.asTextChunkViewModel(parentDoc: TextDoc?, embeddingModelId: String?, score: Float? = null) =
    TextChunkViewModelImpl(parentDoc, this, embeddingModelId, score)

/** Wrap [TextChunk] as a view model. */
class TextChunkViewModelImpl(parentDoc: TextDoc?, val chunk: TextChunk, embeddingModelId: String?, override val score: Float? = null) : TextChunkViewModel {
    constructor(text: String) : this(null, TextChunkRaw(text), null)

    override val browsable = parentDoc?.browsable()
    override val embedding = chunk.getEmbeddingInfo(embeddingModelId ?: "")
    override val text = chunk.text(parentDoc?.all)
}
