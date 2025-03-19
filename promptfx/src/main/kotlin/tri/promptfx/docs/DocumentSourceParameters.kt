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
package tri.promptfx.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.HBox
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.*
import tri.promptfx.library.TextLibraryInfo
import tri.util.ui.graphic
import tri.util.ui.sliderwitheditablelabel
import java.awt.Desktop
import java.io.File
import java.nio.file.Files

/** Add parameters for selection of document sources to a view. */
fun AiTaskView.documentsourceparameters(
    library: SimpleObjectProperty<TextLibrary>,
    documentFolder: SimpleObjectProperty<File>,
    maxChunkSize: SimpleIntegerProperty,
    reindexOp: suspend () -> Unit
) {
    parameters("Document Source and Sectioning") {
        field("Use Folder") {
            enableWhen(library.isNull())
            (inputContainer as? HBox)?.spacing = 5.0
            hyperlink(documentFolder.stringBinding {
                val path = it!!.absolutePath
                if (path.length > 25) {
                    "..." + path.substring(path.length - 24)
                } else {
                    path
                }
            }) {
                action {
                    Files.createDirectories(documentFolder.get().toPath())
                    Desktop.getDesktop().open(documentFolder.get())
                }
            }
            button("", FontAwesomeIcon.FOLDER_OPEN.graphic) {
                tooltip("Select folder with documents to scrape")
                action {
                    promptFxDirectoryChooser("Select folder") { documentFolder.set(it) }
                }
            }
            button("", FontAwesomeIcon.GLOBE.graphic) {
                tooltip("Enter a website to scrape")
                action {
                    val dialog = find<TextCrawlDialog>()
                    dialog.model.webTargetFolder.set(documentFolder.get())
                    dialog.openModal()
                }
            }
            button("", FontAwesomeIcon.REFRESH.graphic) {
                tooltip("Rebuild embedding index for this folder")
                action {
                    // confirm with user then refresh
                    confirm("Rebuild Embedding Index",
                        "Are you sure you want to rebuild the entire embedding index?\n" +
                                "This may require significant API usage and cost.") {
                        runAsync {
                            runBlocking { reindexOp() }
                        }
                    }
                }
            }
        }
        field("Max snippet chars") {
            enableWhen(library.isNull())
            tooltip("Maximum number of characters to include in a chunked section of the document for the embedding index.\n" +
                    "This will only apply to newly chunked documents.")
            sliderwitheditablelabel(200..10000, maxChunkSize)
        }
        field("Use Library") {
            (inputContainer as? HBox)?.spacing = 5.0
            label(library.stringBinding { it?.metadata?.id ?: it?.metadata?.path ?: "None" })
            button("", FontAwesomeIcon.UPLOAD.graphic) {
                tooltip("Select a library to use for document sectioning")
                action {
                    promptFxFileChooser(
                        dirKey = PromptFxConfig.DIR_KEY_TEXTLIB,
                        title = "Load Text Library",
                        filters = arrayOf(PromptFxConfig.FF_JSON, PromptFxConfig.FF_ALL),
                        mode = FileChooserMode.Single
                    ) {
                        it.firstOrNull()?.let {
                            val libInfo = loadLibraryFrom(it)
                            library.set(libInfo.library)
                        }
                    }
                }
            }
            button("", FontAwesomeIcon.SEND.graphic) {
                enableWhen(library.isNotNull)
                tooltip("Open the current library in the Text Manager view")
                action {
                    find<PromptFxWorkspace>().launchTextManagerView(library.get())
                }
            }
            button("", FontAwesomeIcon.MINUS_CIRCLE.graphic) {
                enableWhen(library.isNotNull())
                tooltip("Clear the current library")
                action { library.set(null) }
            }
        }
    }
}

private fun loadLibraryFrom(file: File): TextLibraryInfo {
    val lib = TextLibrary.loadFrom(file)
    if (lib.metadata.id.isBlank())
        lib.metadata.id = file.name
    return TextLibraryInfo(lib, file)
}
