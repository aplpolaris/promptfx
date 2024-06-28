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
package tri.promptfx.library

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableStringValue
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.embedding.cosineSimilarity
import tri.ai.pips.*
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.process.LocalFileManager.extractMetadata
import tri.ai.text.chunks.process.TextDocEmbeddings.addEmbeddingInfo
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.promptfx.PromptFxController
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.ui.TextChunkViewModel
import tri.promptfx.ui.TextChunkViewModelImpl
import tri.promptfx.ui.asTextChunkViewModel
import tri.util.info
import tri.util.pdf.PdfUtils
import tri.util.ui.createListBinding
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/** Model for [TextLibraryView]. */
class TextLibraryViewModel : Component(), ScopedInstance, TextLibraryReceiver {

    private val controller: PromptFxController by inject()
    private val embeddingService
        get() = controller.embeddingService

    val libraryList = observableListOf<TextLibraryInfo>()
    val librarySelection = SimpleObjectProperty<TextLibraryInfo>()

    val librariesModified = observableListOf<TextLibraryInfo>()
    val libraryContentChange = SimpleBooleanProperty() // trigger when library content values change

    val docList: ObservableList<TextDoc> = createListBinding(librarySelection) { it?.library?.docs ?: listOf() }
    val docSelection = observableListOf<TextDoc>()
    val docSelectionPdf = SimpleObjectProperty<TextDoc>(null)
    val docSelectionImages = observableListOf<Image>()

    val docsModified = observableListOf<TextDoc>()
    val documentContentChange = SimpleBooleanProperty() // trigger when document content values change

    val chunkFilter = SimpleObjectProperty<(TextChunk) -> Float>(null)
    val isChunkFilterEnabled = SimpleBooleanProperty(false)

    val chunkList = observableListOf<TextChunkViewModel>()
    val chunkSelection = observableListOf<TextChunkViewModel>()

    init {
        chunkFilter.onChange {
            refilterChunkList()
        }

        // pull images from selected PDF's, add results incrementally to model
        docSelection.onChange {
            val firstPdf = it.list.firstOrNull { it.pdfFile() != null }
            docSelectionPdf.set(firstPdf)

            docSelectionImages.clear()
            it.list.forEach {
                val pdfFile = it.pdfFile()
                if (pdfFile != null && pdfFile.exists()) {
                    runAsync {
                        PdfUtils.pdfPageInfo(pdfFile).flatMap { it.images }.mapNotNull { it.image }
                            .deduplicated()
                    } ui {
                        if (it.isEmpty()) {
                            info<TextLibraryViewModel>("No images found in ${pdfFile.name}")
                        }
                        docSelectionImages.addAll(it.map { SwingFXUtils.toFXImage(it, null) })
                    }
                }
            }

            refilterChunkList()
        }
    }

    //region DERIVED PROPERTIES

    /** Get value indicating if the library has been modified. */
    fun savedStatusProperty(library: TextLibraryInfo): ObservableStringValue =
        Bindings.createStringBinding({
            librariesModified.contains(library).let {
                if (it) "Modified" else ""
            }
        }, librariesModified)

    /** Get value indicating if the document has been modified. */
    fun savedStatusProperty(doc: TextDoc): ObservableStringValue =
        Bindings.createStringBinding({
            docsModified.contains(doc).let {
                if (it) "Metadata Modified" else ""
            }
        }, docsModified)

    //endregion

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

    //region VIEW FILTERING

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

    //region MUTATORS

    /** Save library to a file. */
    fun saveLibrary(library: TextLibraryInfo, it: File) {
        TextLibrary.saveTo(library.library, it)
        library.file = it
        markSaved(library)
    }

    /** Remove selected documents from library and model. */
    fun removeSelectedDocuments() {
        val selected = docSelection.toList()
        librarySelection.value?.library?.docs?.removeAll(selected)
        docList.removeAll(selected)
        docsModified.removeAll(selected)
        markChanged(librarySelection.value)
    }

    /** Remove selected chunks from document(s). */
    fun removeSelectedChunks() {
        val selected = chunkSelection.toList()
        val selectedChunks = selected.mapNotNull { (it as? TextChunkViewModelImpl)?.chunk }
        val changedDocs = mutableListOf<TextDoc>()
        docSelection.forEach {
            if (it.chunks.removeAll(selectedChunks))
                changedDocs.add(it)
        }
        chunkList.removeAll(selected)
        changedDocs.forEach { markChanged(it) }
    }

    /** Rename selected collection. */
    fun renameCollection(lib: TextLibraryInfo, it: String) {
        lib.library.metadata.id = it
        markChanged(lib)
    }

    /** Calculates embeddings for all selected collections, returning associated task. */
    fun calculateEmbeddingsTask(progress: AiTaskMonitor) = runAsync {
        runBlocking {
            AiPipelineExecutor.execute(calculateEmbeddingsPlan().plan(), progress)
        }
    }

    /** Extracts metadata from all documents in selected collections, returning associated task. */
    fun extractMetadataTask() = runAsync {
        var count = 0
        librarySelection.value.library.docs.forEach {
            val path = it.metadata.path
            if (path != null && File(path).exists()) {
                val md = File(path).extractMetadata()
                if (md.isNotEmpty()) {
                    count++
                    updateMetadata(it, md, isSelect = false)
                }
            }
        }
        "Extracted metadata from $count files."
    }

    /** Copy new metadata values into document and update selection. */
    fun updateMetadata(doc: TextDoc, newMetadataValues: Map<String, Any>, isSelect: Boolean) {
        doc.metadata.replaceAll(newMetadataValues)
        if (isSelect)
            docSelection.setAll(listOf(doc))
        markChanged(doc)
    }

    private fun markChanged(lib: TextLibraryInfo) {
        librariesModified.add(lib)
        libraryContentChange.set(!libraryContentChange.get())
    }

    private fun markSaved(library: TextLibraryInfo) {
        librariesModified.remove(library)
        libraryContentChange.set(!libraryContentChange.get())
        docsModified.removeAll(library.library.docs)
    }

    private fun markChanged(doc: TextDoc) {
        docsModified.add(doc)
        val lib = libraryList.find { it.library.docs.contains(doc) }
        if (lib != null)
            markChanged(lib)
    }

    //endregion

    //region LONG-RUNNING TASKS

    private fun calculateEmbeddingsPlan(): AiPlanner {
        val service = embeddingService.value
        val result = mutableMapOf<TextChunk, List<Double>>()
        return listOf(librarySelection.value).flatMap { it.library.docs }.map { doc ->
            AiTask.task("calculate-embeddings: " + doc.metadata.id) {
                service.addEmbeddingInfo(doc)
                var count = 0
                doc.chunks.forEach {
                    val embed = it.getEmbeddingInfo(service.modelId)
                    if (embed != null) {
                        result[it] = embed
                        count++
                    }
                }
                "Calculated $count embeddings for ${doc.metadata.id}."
            }
        }.aggregate().task("summarize-results") {
            "Calculated ${result.size} total embeddings."
        }.planner
    }

    //endregion

    companion object {
        private const val REFILTER_LIST_MAX_COUNT = 20
        private const val MIN_CHUNK_SIMILARITY = 0.7

        /** Merge metadata from a map into a TextDocMetadata object. */
        internal fun TextDocMetadata.mergeIn(other: Map<String, Any>) {
            other.extract("title", "pdf.title", "doc.title") { title = it }
            other.extract("author", "pdf.author", "doc.author", "docx.author") { author = it }
            other.extractDate("date", "pdf.modificationDate", "pdf.creationDate", "doc.editTime", "docx.modified") { dateTime = it }
            properties.putAll(other)
        }

        private fun Map<String, Any>.extract(vararg keys: String, setter: (String) -> Unit) {
            keys.firstNotNullOfOrNull { get(it) }?.let { setter(it.toString()) }
        }

        private fun Map<String, Any>.extractDate(vararg keys: String, setter: (LocalDateTime) -> Unit) {
            keys.firstNotNullOfOrNull { get(it) }?.let {
                when (it) {
                    is LocalDateTime -> setter(it)
                    is LocalDate -> setter(it.atStartOfDay())
                    else -> {
                        try {
                            setter(LocalDateTime.parse(it.toString()))
                        } catch (e: Exception) {
                            info<TextLibraryCollectionUi>("Could not parse date from ${it.javaClass} $it")
                        }
                    }
                }
            }
        }
    }

}
