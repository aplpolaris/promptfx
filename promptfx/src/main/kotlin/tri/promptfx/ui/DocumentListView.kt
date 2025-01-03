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
package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.application.HostServices
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.BrowsableSource
import tri.promptfx.docs.DocumentOpenInViewer
import tri.util.ui.DocumentUtils
import tri.util.ui.graphic

/**
 * Display a list of documents.
 * Clicking on the document name will open the document in a viewer.
 */
class DocumentListView(docs: ObservableList<BrowsableSource>, hostServices: HostServices) : Fragment("Document List") {
    override val root = listview(docs) {
        vgrow = Priority.ALWAYS
        prefHeight = 200.0
        cellFormat {
            graphic = hyperlink(it.shortNameWithoutExtension, graphic = it.icon()) {
                val thumb = DocumentUtils.documentThumbnail(it, DOC_THUMBNAIL_SIZE)
                if (thumb != null) {
                    tooltip { graphic = ImageView(thumb) }
                }
                action { DocumentOpenInViewer(it, hostServices).open() }
            }
        }
    }

    companion object {
        const val DOC_THUMBNAIL_SIZE = 240

        //** Return an icon for the document based on its file extension. */
        fun BrowsableSource.icon() = when (path.substringAfterLast('.')) {
            "pdf" -> FontAwesomeIcon.FILE_PDF_ALT.graphic
            "doc", "docx" -> FontAwesomeIcon.FILE_WORD_ALT.graphic
            "csv", "xls", "xlsx" -> FontAwesomeIcon.FILE_EXCEL_ALT.graphic
            "ppt", "pptx" -> FontAwesomeIcon.FILE_POWERPOINT_ALT.graphic
            "txt" -> FontAwesomeIcon.FILE_TEXT_ALT.graphic
            "html", "htm" -> FontAwesomeIcon.GLOBE.graphic
            else -> {
                if (path.startsWith("html") || path.startsWith("html"))
                    FontAwesomeIcon.GLOBE.graphic
                else
                    FontAwesomeIcon.FILE_ALT.graphic
            }
        }
    }
}
