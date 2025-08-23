package tri.promptfx.ui.docs


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

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import tornadofx.*
import tri.ai.core.EmbeddingModel
import tri.ai.text.chunks.DelimiterTextChunker
import tri.ai.text.chunks.NoOpTextChunker
import tri.ai.text.chunks.RegexTextChunker
import tri.ai.text.chunks.SmartTextChunker
import tri.ai.text.chunks.TextChunk
import tri.ai.text.chunks.TextChunker
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.PromptFxController
import tri.promptfx.ui.chunk.TextChunkViewModel
import tri.promptfx.ui.chunk.asTextChunkViewModel
import java.io.File
import java.net.URL
import java.util.regex.PatternSyntaxException

/** Model for the [TextChunkerWizard]. */
class TextChunkerWizardModel: ViewModel() {

    val controller: PromptFxController by inject()

    // inputs
    var sourceToggleSelection: ObjectProperty<String> = SimpleObjectProperty(TextChunkerSourceMode.FILE.uiName)
    val sourceMode = sourceToggleSelection.objectBinding { TextChunkerSourceMode.valueOfUiName(it) }

    var isFileMode = sourceToggleSelection.isEqualTo(TextChunkerSourceMode.FILE.uiName)!!
    val file = SimpleObjectProperty<File>()
    val fileName = file.stringBinding { it?.name ?: "None" }

    val isFolderMode = sourceToggleSelection.isEqualTo(TextChunkerSourceMode.FOLDER.uiName)!!
    val folder = SimpleObjectProperty<File>()
    val folderName = folder.stringBinding { it?.name ?: "None" }
    val folderIncludeSubfolders = SimpleObjectProperty(false)
    val folderExtractText = SimpleObjectProperty(true)

    val isUserInputMode = sourceToggleSelection.isEqualTo(TextChunkerSourceMode.USER_INPUT.uiName)!!
    val userText = SimpleStringProperty()

    val isWebScrapeMode = sourceToggleSelection.isEqualTo(TextChunkerSourceMode.WEB_SCRAPING.uiName)!!
    val webScrapeModel = WebScrapeViewModel()

    val isRssMode = sourceToggleSelection.isEqualTo(TextChunkerSourceMode.RSS_FEED.uiName)!!
    val rssFeed = SimpleObjectProperty<URL>()

    // whether source has been properly selected
    val isSourceSelected = (isFileMode.and(file.isNotNull))
        .or(isFolderMode.and(folder.isNotNull))
        .or(isUserInputMode.and(userText.isNotEmpty))
        .or(isWebScrapeMode.and(
            webScrapeModel.webUrl.booleanBinding {
                !it.isNullOrBlank() && (it.startsWith("http://") || it.startsWith("https://"))
                        && it.substringAfter("//").isNotBlank()
            }
        ))
        .or(isRssMode.and(rssFeed.isNotNull))!!

    // chunking options
    val isHeaderRow = SimpleBooleanProperty(false)
    var chunkMethodSelection: ObjectProperty<String> = SimpleObjectProperty(TextChunkerWizardMethod.CHUNK_AUTO)
    val isCleanUpWhiteSpace = SimpleBooleanProperty(true)
    val isChunkAutomatic = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_AUTO)!!
    internal val maxChunkSize = SimpleObjectProperty(1000)
    internal val isChunkDelimiter = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_BY_DELIMITER)
    internal val chunkDelimiter = SimpleStringProperty("\n")
    internal val isChunkRegex = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_BY_REGEX)
    internal val chunkRegex = SimpleStringProperty("\\n{2,}")
    internal val isChunkField = chunkMethodSelection.isEqualTo(TextChunkerWizardMethod.CHUNK_BY_FIELD)

    // chunk filter
    val chunkFilterMinSize = SimpleIntegerProperty(50)
    val chunkFilterRemoveDuplicates = SimpleBooleanProperty(true)

    // library location options
    val libraryFolder = SimpleObjectProperty<File>()
    val libraryFileName = SimpleStringProperty("embeddings2.json")
    val extractMetadata = SimpleBooleanProperty(true)
    val generateEmbeddings = SimpleBooleanProperty(false)
    val embeddingModel = SimpleObjectProperty<tri.ai.core.EmbeddingModel>()
    
    // computed properties for library location validation
    val isLibraryLocationValid = libraryFolder.isNotNull
        .and(libraryFileName.isNotEmpty)
        .and(generateEmbeddings.not().or(embeddingModel.isNotNull))
    val libraryFile = Bindings.createObjectBinding(
        { libraryFolder.value?.let { File(it, libraryFileName.value) } },
        libraryFolder, libraryFileName
    )
    val fileExists = libraryFile.booleanBinding { it?.exists() == true }

    // preview of chunks
    val previewChunks = observableListOf<TextChunkViewModel>()

    // whether chunking method has been properly selected
    internal val isChunkMethodValid = Bindings.isNotEmpty(previewChunks)

    init {
        maxChunkSize.onChange { updatePreview() }
        isHeaderRow.onChange { updatePreview() }
        isCleanUpWhiteSpace.onChange { updatePreview() }
        chunkMethodSelection.onChange { updatePreview() }
        chunkDelimiter.onChange { updatePreview() }
        chunkRegex.onChange { updatePreview() }
        chunkFilterMinSize.onChange { updatePreview() }
        
        // Initialize library folder based on source selection
        file.onChange { 
            if (libraryFolder.value == null && it != null) {
                libraryFolder.set(it.parentFile)
            }
        }
        folder.onChange { 
            if (libraryFolder.value == null && it != null) {
                libraryFolder.set(it)
            }
        }
        
        // Initialize embedding model with default
        embeddingModel.set(controller.embeddingStrategy.value?.model)
    }

    /** Get sample of input text based on current settings. */
    private fun inputTextSample(): String {
        val sample = sourceMode.value!!.inputTextSample(this)
        return if (isHeaderRow.value)
            sample.substringAfter("\n").trim()
        else
            sample
    }

    /** Get all text based on current settings, with multiple strings returned if multiple files. */
    private fun inputDocs(progressUpdate: (String) -> Unit) =
        sourceMode.value!!.allInputText(this, progressUpdate)

    /** Get input chunks. */
    private fun inputChunks(): Pair<TextDoc, List<TextChunk>> {
        val inputTextSample = inputTextSample()
        val chunker = chunker()
        val doc = TextDoc("", inputTextSample)
        return doc to chunker.chunkText(doc.all!!.text, maxChunkSize.value)
    }

    /** Initializes chunking options based on input docs. */
    internal fun initChunkingOptions() {
        val (doc, chunks) = inputChunks()
        val maxLength = chunks.maxOfOrNull { it.text(doc.all).length } ?: 1
        if (maxLength < 100) {
            chunkMethodSelection.set(TextChunkerWizardMethod.CHUNK_BY_DELIMITER)
            chunkFilterMinSize.set(1)
        }
    }

    /** Update the preview of chunks based on the current settings. */
    internal fun updatePreview() {
        val MAX_PREVIEW_CHUNKS = 100
        val (doc, chunks) = inputChunks()
        val useChunks = chunks
            .filter(chunkFilter(doc.all!!))
            .take(MAX_PREVIEW_CHUNKS)
        previewChunks.setAll(useChunks.map { it.asTextChunkViewModel(doc, controller.embeddingStrategy.value?.modelId, null) })
    }

    /** Chunker based on current settings. */
    private fun chunker(): TextChunker = when {
        isChunkAutomatic.get() ->
            SmartTextChunker()
        isChunkDelimiter.get() ->
            DelimiterTextChunker(
                isCleanUpWhiteSpace.value, listOf(
                    chunkDelimiter.value
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                        .ifEmpty { "\n" })
            )
        isChunkRegex.get() -> try {
            RegexTextChunker(isCleanUpWhiteSpace.value, chunkRegex.value.toRegex())
        } catch (x: PatternSyntaxException) {
            // TODO - show error to user
            NoOpTextChunker
        }
        isChunkField.get() -> NoOpTextChunker // TODO
        else -> NoOpTextChunker
    }

    /** Get chunk filter. */
    fun chunkFilter(doc: TextChunk?): (TextChunk) -> Boolean = {
        it.text(doc).length >= chunkFilterMinSize.value
    }

    /** Get final chunks. */
    fun finalDocs(progressUpdate: (String) -> Unit): List<TextDoc> {
        val chunker = chunker()
        val addedChunks = mutableSetOf<String>()
        return inputDocs(progressUpdate).mapNotNull { doc ->
            val docChunk = doc.all!!
            val docChunks = chunker.chunkText(docChunk.text, maxChunkSize.value)
                .filter(chunkFilter(docChunk))
            if (!chunkFilterRemoveDuplicates.value)
                doc.chunks.addAll(docChunks)
            else
                docChunks.forEach {
                    val chunkText = it.text(docChunk)
                    if (chunkText !in addedChunks) {
                        doc.chunks.add(it)
                        addedChunks.add(chunkText)
                    }
                }
            if (doc.chunks.isNotEmpty()) doc else null
        }
    }

    fun finalLibrary(progressUpdate: (String) -> Unit): TextLibrary? {
        val sourceInfo = when {
            isFileMode.get() -> file.value?.toURI()?.toString()
            isFolderMode.get() -> folder.value?.toURI()?.toString()
            isUserInputMode.get() -> "User Input"
            isWebScrapeMode.get() -> webScrapeModel.webUrl.value
            isRssMode.get() -> rssFeed.value.toString()
            else -> "Unknown"
        }
        val finalDocs = finalDocs(progressUpdate)
        return if (finalDocs.isEmpty()) null
        else TextLibrary().apply {
            metadata.id = "Text Content from $sourceInfo"
            metadata.path = sourceInfo
            docs.addAll(finalDocs)
        }
    }

}
