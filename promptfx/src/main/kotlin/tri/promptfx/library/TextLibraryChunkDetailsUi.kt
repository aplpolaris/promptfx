package tri.promptfx.library

import tornadofx.*
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.buildsendresultmenu

/** View for showing details of selected chunks. */
class TextLibraryChunkDetailsUi : Fragment() {

    val model by inject<TextLibraryViewModel>()

    override val root = form {
        fieldset("") { }
        vbox {
            bindChildren(model.chunkSelection) { chunk ->
                fieldset("") {
                    val text = chunk.text.trim()
                    fieldifnotblank("Text", text) {
                        contextmenu {
                            item("Find similar chunks") {
                                action { model.createSemanticFilter(text) }
                            }
                            buildsendresultmenu(text, workspace as PromptFxWorkspace)
                        }
                    }
                    fieldifnotblank("Score", chunk.score?.toString())
                    fieldifnotblank("Embeddings", chunk.embeddingsAvailable.joinToString(", "))
                }
            }
        }
    }

}