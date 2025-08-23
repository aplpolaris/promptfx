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
package tri.promptfx.ui.docs

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableStringValue
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.runBlocking
import tornadofx.Component
import tornadofx.ScopedInstance
import tornadofx.observableListOf
import tornadofx.onChange
import tri.ai.pips.*
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.TextLibrary
import tri.ai.text.chunks.TextDocEmbeddings.addEmbeddingInfo
import tri.ai.text.chunks.TextDocEmbeddings.getEmbeddingInfo
import tri.promptfx.PromptFxController
import tri.promptfx.TextLibraryReceiver
import tri.promptfx.docs.TextLibraryInfo
import tri.promptfx.ui.chunk.TextChunkListModel
import tri.promptfx.ui.chunk.TextChunkViewModelImpl
import tri.util.info
import tri.util.io.LocalFileManager.extractMetadata
import tri.util.io.pdf.PdfImageCache
import tri.util.ui.createListBinding
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.associateBy
import kotlin.collections.find
import kotlin.collections.firstNotNullOfOrNull
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toList

/** Model for views that depend on a "library" of documents. */
class TextLibraryViewModel : Component(), ScopedInstance, TextLibraryReceiver {

    private val controller: PromptFxController by inject()
    private val embeddingStrategy
        get() = controller.embeddingStrategy

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

    val chunkListModel: TextChunkListModel by inject()

    init {
        // pull images from selected PDF's, add results incrementally to model
        docSelection.onChange {
            val firstPdf = it.list.firstOrNull { it.pdfFile() != null }

            if (docSelectionPdf.value != firstPdf) {
                docSelectionPdf.set(firstPdf)
                docSelectionImages.clear()
                it.list.forEach {
                    val pdfFile = it.pdfFile()
                    if (pdfFile != null && pdfFile.exists()) {
                        runAsync {
                            PdfImageCache.getImagesFromPdf(pdfFile)
//                        PdfUtils.pdfPageInfo(pdfFile).flatMap { it.images }.mapNotNull { it.image }
//                            .deduplicated()
                        } ui {
                            if (it.isEmpty()) {
                                info<TextLibraryViewModel>("No images found in ${pdfFile.name}")
                            }
                            docSelectionImages.addAll(it.map { SwingFXUtils.toFXImage(it, null) })
                        }
                    }
                }
            }

            updateChunkList()
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
        loadTextLibrary(library, replace = true, selectAllDocs = true)
    }

    /** Load a library from a file, with options to replace existing libraries and select all documents. */
    internal fun loadLibraryFrom(file: File, replace: Boolean, selectAllDocs: Boolean) {
        runAsync {
            val lib = TextLibrary.loadFrom(file)
            if (lib.metadata.id.isBlank())
                lib.metadata.id = file.name
            TextLibraryInfo(lib, file)
        } ui {
            loadTextLibrary(it, replace, selectAllDocs)
        }
    }

    /** Load a library from a library object, with options to replace existing libraries and select all documents. */
    fun loadTextLibrary(library: TextLibraryInfo, replace: Boolean, selectAllDocs: Boolean) {
        if (replace)
            libraryList.setAll(library)
        else if (library !in libraryList) {
            libraryList.add(library)
        }
        librarySelection.set(library)
        if (selectAllDocs)
            docSelection.setAll(library.library.docs)
        else
            docSelection.setAll(library.library.docs.first())
    }

    private fun List<BufferedImage>.deduplicated() =
        associateBy { it.hashCode() }.values.toList()

    //endregion

    //region VIEW FILTERING

    private fun updateChunkList() {
        chunkListModel.setChunkList(docSelection.flatMap { doc -> doc.chunks.map { it to doc } })
    }

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
        val selected = chunkListModel.chunkSelection.toList()
        val selectedChunks = selected.mapNotNull { (it as? TextChunkViewModelImpl)?.chunk }
        val changedDocs = mutableListOf<TextDoc>()
        docSelection.forEach {
            if (it.chunks.removeAll(selectedChunks))
                changedDocs.add(it)
        }
        updateChunkList()
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
            AiPipelineExecutor.execute(calculateEmbeddings().plan, progress)
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

    /** Get tasks that can be used to calculate any missing embeddings for the selected embedding service. */
    fun calculateEmbeddings(): AiTaskList<String> {
        val service = embeddingStrategy.value
        val result = mutableMapOf<TextChunk, List<Double>>()
        return listOf(librarySelection.value).flatMap { it.library.docs }.map { doc ->
            AiTask.task("calculate-embeddings: " + doc.metadata.id) {
                if (doc.chunks.any { it.getEmbeddingInfo(service.modelId) == null })
                    service.model.addEmbeddingInfo(doc)
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
        }
    }

    //endregion

    companion object {
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
                            info<TextLibraryListUi>("Could not parse date from ${it.javaClass} $it")
                        }
                    }
                }
            }
        }
    }

}

