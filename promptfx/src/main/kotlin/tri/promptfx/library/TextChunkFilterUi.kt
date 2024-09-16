package tri.promptfx.library

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import tornadofx.*
import tri.util.ui.graphic

/** View for creating chunk filters. */
class TextChunkFilterUi : Fragment(), ScopedInstance {

    val model by inject<TextLibraryViewModel>()

    private val filter: TextChunkFilterModel by inject()
    private val filterText = SimpleStringProperty("")

    private val filterFlags = FilterType.values().associateWith { SimpleBooleanProperty(false) }.also {
        it[FilterType.NONE]!!.set(true)
    }

    override val root = vbox {
        toolbar {
            text("Search/Filter:")
            togglegroup {
                radiobutton("none") {
                    filterFlags[FilterType.NONE]!!.bindBidirectional(selectedProperty())
                    tooltip("Use this to clear all filters.")
                }
                radiobutton("find") {
                    filterFlags[FilterType.SEARCH]!!.bindBidirectional(selectedProperty())
                    tooltip("Use this to search for an exact substring match.")
                }
                radiobutton("regex") {
                    isSelected = true
                    filterFlags[FilterType.REGEX]!!.bindBidirectional(selectedProperty())
                    tooltip("Use this to find a regex match (case-insensitive).")
                }
                radiobutton("embedding") {
                    filterFlags[FilterType.EMBEDDING]!!.bindBidirectional(selectedProperty())
                    tooltip("Use this to find embedding between query and chunks. Will limit response to top 20 embedding matches.")
                }
                // TODO - wildcard filters
                // TODO - prompt filters
            }
            spacer()
            button("", FontAwesomeIcon.FILTER.graphic) {
                tooltip("Apply search/filter")
                action { updateFilter() }
            }
            button("", FontAwesomeIcon.CLOSE.graphic) {
                tooltip("Clear text filter and rankings")
                action {
                    filterFlags[FilterType.NONE]!!.set(true)
                    filterText.set("")
                    updateFilter()
                }
            }
        }
        textarea(filterText) {
            disableWhen(filterFlags[FilterType.NONE]!!)
            promptText = "Enter text to find/match/filter"
            hgrow = Priority.ALWAYS
            prefRowCount = 5
            isWrapText = true
            prefWidth = 0.0
        }
    }

    /** Creates a semantic filter based on the given chunk. */
    fun createSemanticFilter(text: String) {
        filterText.set(text)
        filterFlags[FilterType.EMBEDDING]!!.set(true)
        updateFilter()
    }

    private fun updateFilter() {
        val type = filterFlags.entries.first { it.value.get() }.key
        filter.updateFilter(type, filterText.get(), model.chunkList)
    }

}