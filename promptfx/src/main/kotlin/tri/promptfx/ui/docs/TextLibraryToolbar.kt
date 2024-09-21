package tri.promptfx.ui.docs

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.util.ui.graphic

/** A toolbar for users to create or select an existing library. */
class TextLibraryToolbar : Fragment() {

    private val libraryModel by inject<TextLibraryViewModel>()
    val separateLibraryLabel: Boolean by param(false)
    val titleText = SimpleStringProperty("Collection")
    val showButtonText = SimpleBooleanProperty(false)
    private val libraryName = libraryModel.librarySelection.stringBinding {
        it?.library?.metadata?.id ?: "No collection selected"
    }

    override val root = vbox {
        toolbar {
            text(titleText)
            spacer()
            if (!separateLibraryLabel) {
                text(libraryName) {
                    style = "-fx-font-style: italic;"
                }
            }
            // open wizard to create a new TextLibrary
            button(textIf("Create...", showButtonText), FontAwesomeIcon.PLUS_CIRCLE.graphic) {
                tooltip("Create a new text collection.")
                action { createLibraryWizard(libraryModel, replace = true, selectAllDocs = true) }
            }
            // load a TextLibrary file
            button(textIf("Load...", showButtonText), FontAwesomeIcon.FOLDER_OPEN.graphic) {
                tooltip("Load a text collection from a JSON file.")
                action { loadLibrary(libraryModel, replace = true, selectAllDocs = true) }
            }
            // send the library to TextManager view
            button("", FontAwesomeIcon.SEND.graphic) {
                enableWhen(libraryModel.librarySelection.isNotNull)
                tooltip("Open the current library in the Text Manager view")
                action {
                    (workspace as PromptFxWorkspace).launchTextManagerView(libraryModel.librarySelection.get().library)
                }
            }
        }
        if (separateLibraryLabel) {
            text(libraryName) {
                style = "-fx-font-style: italic;"
            }
        }
    }

    private fun textIf(text: String, show: SimpleBooleanProperty) = Bindings.`when`(show).then(text).otherwise("")

}