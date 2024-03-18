package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Binding
import javafx.beans.binding.ListBinding
import javafx.beans.binding.ListExpression
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import tornadofx.*
import tri.ai.prompt.AiPromptLibrary
import tri.ai.text.chunks.*
import tri.promptfx.AiTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.createListBinding


/** Plugin for the [TextManagerView]. */
class TextManagerPlugin : NavigableWorkspaceViewImpl<TextManagerView>("Tools", "Text Manager", TextManagerView::class)

/** A view designed to help you manage collections of documents and text. */
class TextManagerView : AiTaskView("Text Manager", "Manage collections of documents and text.") {

    private val libraryList = observableListOf<TextLibrary>()
    private val librarySelection = SimpleObjectProperty<TextLibrary>()
    private val docList = createListBinding(librarySelection) { it?.books ?: listOf() }
    private val docSelection = SimpleObjectProperty<TextBook>()
    private val chunkList = createListBinding(docSelection) { it?.chunks ?: listOf() }
    private val chunkSelection = SimpleObjectProperty<TextChunk>()

    init {
        toolbar {
            //
        }
        listview(libraryList) {
            vgrow = Priority.ALWAYS
            librarySelection.bind(selectionModel.selectedItemProperty())
            cellFormat {
                graphic = Text(it.toString())
            }
        }
        listview(docList) {
            vgrow = Priority.ALWAYS
            docSelection.bind(selectionModel.selectedItemProperty())
            cellFormat {
                graphic = Text(it.toString())
            }
        }
        listview(chunkList) {
            vgrow = Priority.ALWAYS
            chunkSelection.bind(selectionModel.selectedItemProperty())
            cellFormat {
                graphic = Text(it.toString())
            }
        }
    }
}