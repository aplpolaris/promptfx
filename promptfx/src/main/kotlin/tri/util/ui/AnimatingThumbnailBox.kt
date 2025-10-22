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
package tri.util.ui

import javafx.beans.property.SimpleIntegerProperty
import javafx.event.EventTarget
import javafx.scene.image.Image
import javafx.scene.layout.HBox
import tornadofx.*
import tri.ai.text.chunks.BrowsableSource

/** Box with an animating set of thumbnails. */
class AnimatingThumbnailBox(_action: ((DocumentThumbnail) -> Unit)?) : HBox() {

    private val thumbnailList = observableListOf<DocumentThumbnail>()
    private val thumbnailAction: ((DocumentThumbnail) -> Unit)? = _action

    init {
        children.bind(thumbnailList) { docthumbnail(it) }
    }

    private fun EventTarget.docthumbnail(doc: DocumentThumbnail) = vbox {
        if (doc.image == null)
            hyperlink(doc.document.shortName) {
                style = "-fx-font-size: 16px;"
                thumbnailAction?.let {
                    action { it(doc) }
                }
            }
        else
            imageview(doc.image) {
                opacity = 0.0
                timeline {
                    keyframe(1.0.seconds) {
                        keyvalue(scaleXProperty(), 1.1)
                        keyvalue(scaleYProperty(), 1.1)
                    }
                    keyframe(2.0.seconds) {
                        keyvalue(opacityProperty(), 1.0)
                        keyvalue(scaleXProperty(), 1.0)
                        keyvalue(scaleYProperty(), 1.0)
                    }
                }
                thumbnailAction?.let {
                    cursor = javafx.scene.Cursor.HAND
                    setOnMouseClicked { it(doc) }
                }
            }
    }

    /** Updates the displayed thumbnails, animating the transition. */
    fun animateThumbs(thumbs: List<DocumentThumbnail>) {
        thumbnailList.clear()
        val entries = thumbs.toList()
        val n = SimpleIntegerProperty(-1).apply {
            onChange { thumbnailList.add(DocumentThumbnail(entries[it].document, entries[it].image)) }
        }
        timeline {
            keyframe(2.0.seconds) {
                keyvalue(n, entries.size - 1)
            }
        }
    }
}

/** A document thumbnail object. */
class DocumentThumbnail(val document: BrowsableSource, val image: Image?)
