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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.HostServices
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import tornadofx.action
import tornadofx.hyperlink
import tornadofx.text
import tornadofx.tooltip
import tri.promptfx.docs.DocumentBrowseToPage
import tri.util.ui.DocumentUtils

/** View for a [TextChunkViewModel]. */
class TextChunkView(it: TextChunkViewModel, hostServices: HostServices) : HBox() {
    init {
        spacing = 5.0
        alignment = Pos.CENTER_LEFT
        it.embedding?.let {
            children.add(FontAwesomeIconView(FontAwesomeIcon.MAP_MARKER))
        }
        it.score?.let { score ->
            text("%.2f".format(score)) {
                style = "-fx-font-weight: bold;"
            }
        }
        it.browsable?.let { doc ->
            hyperlink(doc.shortNameWithoutExtension) {
                val thumb = DocumentUtils.documentThumbnail(doc)
                if (thumb != null) {
                    tooltip { graphic = ImageView(thumb) }
                }
                action {
                    DocumentBrowseToPage(doc, it.text, hostServices).open()
                }
            }
        }

        val text = it.text
        val shortText = (if (text.length <= 80) text else "${text.take(80)}...").replace("\n", " ").replace("\r", " ").trim()
        text(shortText) {
            tooltip(text) {
                maxWidth = 500.0
                isWrapText = true
            }
        }
    }
}
