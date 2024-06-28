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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.layout.Priority
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.apps.ImageDescribeView
import tri.util.ui.copyToClipboard
import tri.util.ui.graphic
import tri.util.ui.saveToFile
import tri.util.ui.showImageDialog

/** View showing image contents for selected set of documents. */
class TextLibraryDocumentImagesUi : Fragment() {

    private val model by inject<TextLibraryViewModel>()
    private val thumbnailSize = SimpleDoubleProperty(128.0)

    override val root = datagrid(model.docSelectionImages) {
        vgrow = Priority.ALWAYS
        prefWidth = 600.0
        prefHeight = 600.0
        cellWidthProperty.bind(thumbnailSize)
        cellHeightProperty.bind(thumbnailSize)
        cellCache {
            imageview(it) {
                fitWidthProperty().bind(thumbnailSize)
                fitHeightProperty().bind(thumbnailSize)
                isPreserveRatio = true
                isPickOnBounds = true // so you can click anywhere on transparent images
                tooltip { graphic = imageview(it) }
                contextmenu {
                    item("View full size").action { showImageDialog(image) }
                    item("Copy to clipboard").action { copyToClipboard(image) }
                    item("Send to Image Description View", graphic = FontAwesomeIcon.SEND.graphic) {
                        action {
                            val view = (workspace as PromptFxWorkspace).findTaskView("Image Description")
                            (view as? ImageDescribeView)?.apply {
                                setImage(image)
                                workspace.dock(view)
                            }
                        }
                    }
                    item("Save to file...").action { saveToFile(image) }
                }
            }
        }
    }
}
