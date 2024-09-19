package tri.promptfx.ui.chunk

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.scene.layout.Priority
import tornadofx.*
import tri.promptfx.PromptFxController
import tri.promptfx.ui.chunk.TextChunkFilterModel.Companion.selectWhen
import tri.util.ui.graphic

/** View for creating chunk filters. */
class TextChunkFilterUi : Fragment(), ScopedInstance {

    val controller: PromptFxController by inject()
    val listModel: TextChunkListModel by inject()
    private val filterModel: TextChunkFilterModel by inject()

    override val root = vbox {
        toolbar {
            text("Search/Filter:")
            togglegroup {
                radiobutton("none") {
                    selectWhen(filterModel, TextFilterType.NONE)
                    tooltip("Use this to clear all filters.")
                }
                radiobutton("find") {
                    selectWhen(filterModel, TextFilterType.SEARCH)
                    tooltip("Use this to search for an exact substring match.")
                }
                radiobutton("regex") {
                    selectWhen(filterModel, TextFilterType.REGEX)
                    tooltip("Use this to find a regex match (case-insensitive).")
                }
                radiobutton("embedding") {
                    selectWhen(filterModel, TextFilterType.EMBEDDING)
                    tooltip("Use this to find embedding between query and chunks. Will limit response to top 20 embedding matches.")
                }
                // TODO - wildcard filters
                // TODO - prompt filters
            }
            spacer()
            button("Apply", FontAwesomeIcon.CHECK.graphic) {
                enableWhen(filterModel.isFilterUnapplied)
                tooltip("Apply search/filter")
                action { updateFilter() }
            }
            button("Clear", FontAwesomeIcon.CLOSE.graphic) {
                enableWhen(filterModel.filterType.isNotEqualTo(TextFilterType.NONE))
                tooltip("Clear text filter and rankings")
                action {
                    filterModel.disableFilter(listModel.chunkList)
                }
            }
        }
        textarea(filterModel.filterText) {
            disableWhen(filterModel.filterType.isEqualTo(TextFilterType.NONE))
            promptText = "Enter text to find/match/filter"
            hgrow = Priority.ALWAYS
            prefRowCount = 5
            isWrapText = true
            prefWidth = 0.0
        }
    }

    private fun updateFilter() {
        filterModel.updateFilter(listModel.chunkList, controller.embeddingService.value)
    }

}