package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import tornadofx.*
import tri.util.ui.graphic

/** A toolbar for users to create or select an existing library. */
class TextLibraryToolbar : Fragment() {

    private val libraryModel by inject<TextLibraryViewModel>()

    override val root = toolbar {
        text("Collections")
        spacer()
        // generate chunks
        button("Create...", FontAwesomeIconView(FontAwesomeIcon.PLUS)) {
            tooltip("Create a new text collection.")
            action { createLibraryWizard(libraryModel, replace = true, selectAllDocs = true) }
        }
        // load a TextLibrary file
        button("Load...", FontAwesomeIconView(FontAwesomeIcon.UPLOAD)) {
            tooltip("Load a text collection from a JSON file.")
            action { loadLibrary(libraryModel, replace = true, selectAllDocs = true) }
        }
    }

}