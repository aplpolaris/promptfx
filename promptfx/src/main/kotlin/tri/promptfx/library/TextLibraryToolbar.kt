package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.util.ui.graphic

/** A toolbar for users to create or select an existing library. */
class TextLibraryToolbar : Fragment() {

    private val libraryModel by inject<TextLibraryViewModel>()
    val titleText = SimpleStringProperty("Collection")
    val showButtonText = SimpleBooleanProperty(false)
    val libraryName = libraryModel.librarySelection.stringBinding {
        it?.library?.metadata?.id ?: ""
    }

    override val root = toolbar {
        text(titleText)
        spacer()
        text(libraryName) {
            style = "-fx-font-style: italic;"
        }
        // open wizard to create a new TextLibrary
        button(textIf("Create...", showButtonText), FontAwesomeIcon.PLUS.graphic) {
            tooltip("Create a new text collection.")
            action { createLibraryWizard(libraryModel, replace = true, selectAllDocs = true) }
        }
        // load a TextLibrary file
        button(textIf("Load...", showButtonText), FontAwesomeIcon.UPLOAD.graphic) {
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

    private fun textIf(text: String, show: SimpleBooleanProperty) = Bindings.`when`(show).then(text).otherwise("")

}