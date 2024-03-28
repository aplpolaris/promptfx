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

import javafx.application.HostServices
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.text.chunks.BrowsableSource
import tri.promptfx.docs.DocumentOpenInViewer
import tri.util.ui.DocumentUtils

/**
 * Display a list of documents.
 * Clicking on the document name will open the document in a viewer.
 */
class DocumentListView(docs: ObservableList<BrowsableSource>, hostServices: HostServices) : Fragment("Document List") {
    override val root = listview(docs) {
        vgrow = Priority.ALWAYS
        cellFormat {
            graphic = hbox {
                textflow {  }
                alignment = Pos.CENTER_LEFT
                hyperlink(it.shortNameWithoutExtension) {
                    val thumb = DocumentUtils.documentThumbnail(it)
                    if (thumb != null) {
                        tooltip { graphic = ImageView(thumb) }
                    }
                    action { DocumentOpenInViewer(it, hostServices).open() }
                }
            }
        }
    }
}
