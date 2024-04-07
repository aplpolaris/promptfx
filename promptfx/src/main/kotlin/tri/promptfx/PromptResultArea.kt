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
package tri.promptfx

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.FileChooser
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TXT
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_TXT
import tri.promptfx.ui.PromptTraceDetails

/**
 * Text area for displaying a prompt result or other output. Adjusts font size, adds ability to copy/save output to a file.
 */
class PromptResultArea : Fragment("Prompt Result Area") {

    private val text = SimpleStringProperty("")
    val trace = SimpleObjectProperty<AiPromptTrace>(null)

    fun setFinalResult(finalResult: AiPromptTrace) {
        text.set(finalResult.outputInfo.output ?: "(No result)")
        trace.set(finalResult as? AiPromptTrace)
    }

    override val root = textarea(text) {
        promptText = "Prompt output will be shown here"
        isEditable = false
        isWrapText = true
        font = Font("Segoe UI Emoji", 18.0)
        vgrow = Priority.ALWAYS

        // add context menu option to save result to a file
        contextmenu {
            item("Details...") {
                enableWhen { trace.isNotNull }
                action {
                    find<PromptTraceDetails>().apply {
                        setTrace(trace.get())
                        openModal()
                    }
                }
            }
            item("Try in template view") {
                enableWhen(trace.booleanBinding { it != null && it.promptInfo.prompt.isNotBlank() })
                action {
                    (workspace as PromptFxWorkspace).launchTemplateView(trace.value)
                }
            }
            separator()
            item ("Select all") {
                action { selectAll() }
            }
            item("Copy") {
                action { copy() }
            }
            item("Save to file...") {
                action {
                    promptFxFileChooser(
                        dirKey = DIR_KEY_TXT,
                        title = "Save to File",
                        filters = arrayOf(FF_TXT, FF_ALL),
                        mode = FileChooserMode.Save
                    ) {
                        it.firstOrNull()?.writeText(selectedText.ifBlank { this@textarea.text })
                    }
                }
            }
        }
    }

}
