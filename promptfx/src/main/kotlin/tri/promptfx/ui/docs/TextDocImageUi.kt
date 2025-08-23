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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.multimodal.ImageDescribeView
import tri.util.ui.copyToClipboard
import tri.util.ui.graphic
import tri.util.ui.saveToFile
import tri.util.ui.showImageDialog
import tri.util.warning

/** View showing image contents for selected set of documents. */
class TextDocImageUi: Fragment() {

    val images: ObservableList<Image> by param()

    private val thumbnailSize = SimpleDoubleProperty(128.0)

    override val root = scrollpane {
        vgrow = Priority.ALWAYS
        hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        isFitToHeight = true
        
        // Set preferred size based on whether there are images and thumbnail size
        // If no images, use minimal size; otherwise use width for ~4 images + spacing
        prefWidthProperty().bind(
            Bindings.createDoubleBinding({
                if (images.isEmpty()) 100.0 else thumbnailSize.value * 4.2
            }, images, thumbnailSize)
        )
        prefHeightProperty().bind(
            Bindings.createDoubleBinding({
                if (images.isEmpty()) 50.0 else thumbnailSize.value + 20.0
            }, images, thumbnailSize)
        )
        
        hbox {
            spacing = 8.0
            
            // Bind to the observable list to handle dynamic updates
            bindChildren(images) { image ->
                imageview(image) {
                    fitWidthProperty().bind(thumbnailSize)
                    fitHeightProperty().bind(thumbnailSize)
                    isPreserveRatio = true
                    isPickOnBounds = true // so you can click anywhere on transparent images
                    tooltip { graphic = imageview(image) }
                    contextmenu {
                        item("View full size").action { showImageDialog(image) }
                        item("Copy to clipboard").action { copyToClipboard(image) }
                        item("Send to Image Description View", graphic = FontAwesomeIcon.SEND.graphic) {
                            action {
                                val workspace = scope!!.workspace as PromptFxWorkspace
                                val view = try {
                                    workspace.findTaskView<ImageDescribeView>()!!
                                } catch (e: Exception) {
                                    warning<TextDocImageUi>("Unable to find image description view.", e)
                                    return@action
                                }
                                view.setImage(image)
                                workspace.dock(view)
                                view.runTask()
                            }
                        }
                        item("Save to file...").action { saveToFile(image) }
                    }
                }
            }
        }
    }
}
