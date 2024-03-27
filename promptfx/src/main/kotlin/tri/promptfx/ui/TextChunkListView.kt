package tri.promptfx.ui

import javafx.application.HostServices
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import tornadofx.Fragment
import tornadofx.listview
import tornadofx.vgrow

/**
 * Display a list of document chunks, along with document thumbnails.
 * Clicking on the document name will open the document in a viewer.
 */
class TextChunkListView(snippets: ObservableList<TextChunkViewModel>, hostServices: HostServices) : Fragment("TextChunk List") {

    override val root = listview(snippets) {
        vgrow = Priority.ALWAYS
        cellFormat {
            graphic = TextChunkView(it, hostServices)
        }
    }

    fun refresh() {
        root.refresh()
    }

}

