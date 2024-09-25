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
package tri.promptfx.ui.chunk

import javafx.event.EventTarget
import javafx.geometry.Pos
import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.buildsendresultmenu

/** View for showing details of selected chunks. */
class TextChunkDetailsUi : Fragment() {

    private val listModel: TextChunkListModel by inject()

    override val root = form {
        fieldset("") { }
        vbox {
            bindChildren(listModel.chunkSelection) { chunk ->
                fieldset("") {
                    val text = chunk.text.trim()
                    fieldifnotblank("Text", text) {
                        contextmenu {
                            item("Find similar chunks") {
                                action {
                                    listModel.applyEmbeddingFilter(text)
                                }
                            }
                            buildsendresultmenu("chunk text", text, workspace as PromptFxWorkspace)
                        }
                    }
                    fieldifnotblank("Score", chunk.score?.toString())
                    fieldifnotblank("Embeddings", chunk.embeddingsAvailable.joinToString(", "))
                }
            }
        }
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
