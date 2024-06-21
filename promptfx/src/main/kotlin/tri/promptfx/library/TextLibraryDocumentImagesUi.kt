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