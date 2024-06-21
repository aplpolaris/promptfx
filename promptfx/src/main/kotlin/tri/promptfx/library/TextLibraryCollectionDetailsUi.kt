package tri.promptfx.library

import tornadofx.*

/** View for collection details. */
class TextLibraryCollectionDetailsUi : Fragment() {

    val model by inject<TextLibraryViewModel>()

    private val librarySelection = model.librarySelection

    override val root = form {
        val changeProperty = model.libraryContentChange
        val libraryId = librarySelection.stringBinding(changeProperty) { it?.library?.metadata?.id }
        val file = librarySelection.stringBinding(changeProperty) { it?.file?.name ?: "No file" }
        val libraryInfo = librarySelection.stringBinding(changeProperty) { "${it?.library?.docs?.size ?: 0} documents" }

        fieldset("") {
            visibleWhen { librarySelection.isNotNull }
            managedWhen { librarySelection.isNotNull }
            field("Id") { text(libraryId) }
            field("File") { text(file) }
            field("Info") { text(libraryInfo) }
        }
    }

}