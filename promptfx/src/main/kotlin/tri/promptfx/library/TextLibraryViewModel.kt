package tri.promptfx.library

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SingleSelectionModel
import javafx.scene.image.Image
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.cosineSimilarity
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxController
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.promptFxFileChooser
import tri.promptfx.ui.TextChunkViewModel
import tri.promptfx.ui.asTextChunkViewModel
import tri.util.pdf.PdfUtils
import tri.util.ui.createListBinding
import java.awt.image.BufferedImage
import java.io.File

/** Model for [TextLibraryView]. */
class TextLibraryViewModel : Component(), ScopedInstance, TextLibraryReceiver {

    private val controller: PromptFxController by inject()
    private val embeddingService = controller.embeddingService

    val libraryList = observableListOf<TextLibraryInfo>()
    val librarySelection = SimpleObjectProperty<TextLibraryInfo>()
    val libraryIdChange = SimpleBooleanProperty(false)

    val docList: ObservableList<TextDoc> = createListBinding(librarySelection) { it?.library?.docs ?: listOf() }
    val docSelection = observableListOf<TextDoc>()
    val selectedDocImages = observableListOf<Image>()

    val chunkFilter = SimpleObjectProperty<(TextChunk) -> Float>(null)
    val isChunkFilterEnabled = SimpleBooleanProperty(false)

    val chunkList: ObservableList<TextChunkViewModel> = observableListOf()
    val chunkSelection = observableListOf<TextChunkViewModel>()

    init {
        chunkFilter.onChange {
            refilterChunkList()
        }

        // pull images from selected PDF's
        docSelection.onChange {
            selectedDocImages.clear()
            it.list.forEach {
                val browsable = it.browsable()
                val pdfFile = browsable?.file?.let { if (it.extension.lowercase() == "pdf") it else null }
                if (pdfFile != null && pdfFile.exists()) {
                    runAsync {
                        PdfUtils.pdfPageInfo(pdfFile).flatMap { it.images }.mapNotNull { it.image }
                            .deduplicated()
                    } ui {
                        // TODO - for testing ... some images show up in form objects and are not discovered using the code above
                        if (it.isEmpty()) {
                            println("No images found in $this")
                            PdfUtils.pdfPageInfo(pdfFile).flatMap { it.images }.mapNotNull { it.image }
                        }
                        selectedDocImages.addAll(it.map { SwingFXUtils.toFXImage(it, null) })
                    }
                }
            }
            refilterChunkList()
        }
    }

    //region COLLECTION I/O

    override fun loadTextLibrary(library: TextLibraryInfo) {
        if (library !in libraryList) {
            libraryList.add(library)
            librarySelection.set(library)
        }
    }

    internal fun loadLibraryFrom(file: File) {
        runAsync {
            val lib = TextLibrary.loadFrom(file)
            if (lib.metadata.id.isBlank())
                lib.metadata.id = file.name
            TextLibraryInfo(lib, file)
        } ui {
            libraryList.add(it)
            librarySelection.set(it)
        }
    }

    //endregion

    //region MODEL UPDATERS

    /** Create semantic filtering, by returning the cosine similarity of a chunk to the given argument. */
    internal fun createSemanticFilter(text: String) {
        runAsync {
            val model = embeddingService.value
            val embedding = runBlocking { model.calculateEmbedding(text) }
            val filter: (TextChunk) -> Float = { chunk ->
                val chunkEmbedding = chunk.getEmbeddingInfo(model.modelId)
                if (chunkEmbedding == null) 0f else cosineSimilarity(embedding, chunkEmbedding).toFloat()
            }
            filter
        } ui {
            chunkFilter.value = it
            isChunkFilterEnabled.set(true)
        }
    }

    /** Get embedding information, as list of calculated embedding models, within a [TextChunk]. */
    private fun TextChunk.embeddingInfo(): String {
        val models = getEmbeddingInfo()?.keys ?: listOf()
        return if (models.isEmpty()) "No embeddings calculated."
        else models.joinToString(", ") { it }
    }

    /** Refilters the chunk list. */
    fun refilterChunkList() {
        chunkList.clear()
        val filter = chunkFilter.value
        val chunksWithScores = docSelection.flatMap { doc -> doc.chunks.map { doc to it } }.let {
            if (filter == null)
                it.associateWith { null }
            else {
                it.associateWith { filter(it.second) }.entries
                    .sortedByDescending { it.value }
                    .take(REFILTER_LIST_MAX_COUNT)
                    .filter { it.value >= MIN_CHUNK_SIMILARITY }
                    .associate { it.key to it.value }
            }
        }
        chunkList.addAll(chunksWithScores.map {
            it.key.second.asTextChunkViewModel(it.key.first, controller.embeddingService.value.modelId, score = it.value)
        })
    }

    private fun List<BufferedImage>.deduplicated() =
        associateBy { it.hashCode() }.values.toList()

    //endregion

    companion object {
        private const val REFILTER_LIST_MAX_COUNT = 20
        private const val MIN_CHUNK_SIMILARITY = 0.7
    }

}