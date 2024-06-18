package tri.promptfx.library

import tornadofx.*

/** View for collection details. */
class TextLibraryCollectionDetailsUi : Fragment() {

    val model by inject<TextLibraryViewModel>()

    private val librarySelection = model.librarySelection
    private val idChange = model.libraryIdChange

    override val root = form {
        val libraryId = librarySelection.stringBinding(idChange) { it?.library?.metadata?.id }
        val libraryInfo = librarySelection.stringBinding { "${it?.library?.docs?.size ?: 0} documents" }

        fieldset("") {
            visibleWhen { librarySelection.isNotNull }
            managedWhen { librarySelection.isNotNull }
            field("Id") { text(libraryId) }
            field("Info") { text(libraryInfo) }
        }
    }

}