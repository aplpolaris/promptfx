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

import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.TextDoc
import tri.ai.text.chunks.process.TextDocEmbeddings.getEmbeddingInfo
import tri.promptfx.docs.DocumentOpenInViewer
import tri.promptfx.library.fieldifnotblank
import tri.promptfx.ui.DocumentListView
import tri.util.ui.DocumentUtils

/** View for document details, for 1 or more selected documents. */
class TextDocDetailsUi(private val selectedItems: ObservableList<TextDoc>) : Fragment() {

    override val root = vbox(10) {
        hgrow = Priority.ALWAYS
        bindChildren(selectedItems) { doc ->
            val thumb = doc.browsable()?.let {
                DocumentUtils.documentThumbnail(it, DocumentListView.DOC_THUMBNAIL_SIZE)
            }
            hbox(10) {
                if (thumb != null) {
                    imageview(thumb) {
                        fitWidth = 120.0
                        isPreserveRatio = true
                    }
                }
                form {
                    hgrow = Priority.ALWAYS
                    fieldset(doc.metadata.id.substringAfterLast("/")) {
                        hgrow = Priority.ALWAYS
                        fieldifnotblank("Id", doc.metadata.id)
                        fieldifnotblank("Title", doc.metadata.title)
                        fieldifnotblank("Author", doc.metadata.author)
                        fieldifnotblank(
                            "Date",
                            doc.metadata.dateTime?.toString() ?: doc.metadata.date?.toString()
                        )
                        doc.metadata.path?.let {
                            field("Path") {
                                hyperlink(it.toString()) {
                                    action {
                                        DocumentOpenInViewer(
                                            doc.browsable()!!,
                                            hostServices
                                        ).open()
                                    }
                                }
                                it.toString()
                            }
                        }
                        fieldifnotblank("Relative Path", doc.metadata.relativePath)
                        doc.metadata.properties.forEach { (key, value) ->
                            fieldifnotblank(key, value?.toString())
                        }
                        fieldifnotblank("Embeddings", doc.embeddingInfo())
                    }
                }
            }
        }
    }

    /** Get embedding information, as list of calculated embedding models, within a [TextDoc]. */
    private fun TextDoc.embeddingInfo(): String {
        val models = chunks.flatMap { it.getEmbeddingInfo()?.keys ?: listOf() }.toSet()
        return if (models.isEmpty()) "No embeddings calculated."
        else models.joinToString(", ") { it }
    }
}
