package tri.promptfx.ui

import javafx.collections.ObservableList
import tornadofx.observableListOf
import tornadofx.onChange
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.EmbeddingSectionInDocument
import tri.promptfx.docs.SnippetMatch

/** View model for document chunks. */
interface TextChunkViewModel {
    val score: Double?
    val doc: EmbeddingDocument
    val text: String
}

/** Convert an observable list of [SnippetMatch] to a list of [TextChunkViewModel]. */
fun ObservableList<SnippetMatch>.matchViewModel(): ObservableList<TextChunkViewModel> {
    val result = observableListOf(map { it.asTextChunkViewModel()})
    onChange { result.setAll(map { it.asTextChunkViewModel() }) }
    return result
}

/** Convert an observable list of [EmbeddingSectionInDocument] to a list of [TextChunkViewModel]. */
fun ObservableList<EmbeddingSectionInDocument>.sectionViewModel(): ObservableList<TextChunkViewModel> {
    val result = observableListOf(map { it.asTextChunkViewModel()})
    onChange { result.setAll(map { it.asTextChunkViewModel() }) }
    return result
}

/** Wrap [SnippetMatch] as a view model. */
fun SnippetMatch.asTextChunkViewModel() = object : TextChunkViewModel {
    override val score = this@asTextChunkViewModel.score
    override val doc = embeddingMatch.document
    override val text = snippetText
}

/** Wrap [EmbeddingSectionInDocument] as a view model. */
fun EmbeddingSectionInDocument.asTextChunkViewModel() = object : TextChunkViewModel {
    override val score = null
    override val doc = this@asTextChunkViewModel.doc
    override val text = readText()
}
