/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.library

import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.TextLibraryReceiver
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import java.io.File

/** Plugin for the [TextManagerView]. */
class TextManagerPlugin : NavigableWorkspaceViewImpl<TextManagerView>("Documents", "Text Manager", WorkspaceViewAffordance.COLLECTION_ONLY, TextManagerView::class)

/** A view designed to help you manage collections of documents and text. */
class TextManagerView : AiTaskView("Text Manager", "Manage collections of documents and text."), TextLibraryReceiver {

    val model by inject<TextLibraryViewModel>()

    init {
        runButton.isVisible = false
        runButton.isManaged = false
        hideParameters()

        input {
            splitpane {
                add(TextLibraryCollectionListUi())
                add(TextLibraryDocumentListUi())
                add(TextLibraryFilterableChunkListView())
            }
        }

        with (outputPane) {
            clear()
            scrollpane {
                squeezebox(multiselect = true) {
                    vgrow = Priority.ALWAYS
                    fold("Details on Selected Collection", expanded = true) {
                        add(TextLibraryCollectionDetailsUi())
                    }
                    fold("Details on Selected Document(s)", expanded = true) {
                        isFitToWidth = true
                        add(TextLibraryDocumentDetailsUi())
                    }
                    fold("Images from Document", expanded = false) {
                        add(TextLibraryDocumentImagesUi())
                    }
                    fold("Details on Selected Chunk(s)", expanded = false) {
                        vgrow = Priority.ALWAYS
                        add(TextLibraryChunkDetailsUi())
                    }
                }
            }
        }
    }

    init {
        val filesToRestore = find<PromptFxConfig>().libraryFiles()
        filesToRestore.forEach { model.loadLibraryFrom(it) }
    }

    override fun loadTextLibrary(library: TextLibraryInfo) {
        model.loadTextLibrary(library)
    }

    override suspend fun processUserInput() = AiPipelineResult.todo()
}

/** Track a library with where it was loaded from, null indicates not saved to a file. */
data class TextLibraryInfo(val library: TextLibrary, var file: File?)

//region UI BUILDERS

private fun EventTarget.fieldifnotblank(label: String, text: ObservableValue<String>) {
    field(label) {
        text(text) {
            wrappingWidth = 400.0
        }
        managedWhen(text.isNotBlank())
        visibleWhen(text.isNotBlank())
    }
}

internal fun EventTarget.fieldifnotblank(label: String, text: String?, op: Field.() -> Unit = { }) {
    if (!text.isNullOrBlank())
        field(label) {
            labelContainer.alignment = Pos.TOP_LEFT
            text(text)
            op()
        }
}

//endregion
