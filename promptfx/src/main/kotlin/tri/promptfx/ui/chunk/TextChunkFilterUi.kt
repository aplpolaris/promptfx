/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
