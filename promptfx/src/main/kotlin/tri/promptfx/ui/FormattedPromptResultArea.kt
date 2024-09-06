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
package tri.promptfx.ui

import javafx.scene.input.DataFormat
import javafx.scene.layout.Priority
import javafx.scene.text.TextFlow
import tornadofx.*
import tri.util.ui.plainText

/** Formatted text area for displaying a prompt result or other output, based on [TextFlow].
 * Adds support for clickable links, cycling outputs if multiple, and save to file.
 */
class FormattedPromptResultArea : PromptResultAreaSupport("Formatted Prompt Result Area") {

    private val htmlArea = TextFlow()

    override val root = vbox {
        vgrow = Priority.ALWAYS
        addtoolbar()
        scrollpane {
            vgrow = Priority.ALWAYS
            isFitToWidth = true
            htmlArea.apply {
                padding = insets(5.0)
                vgrow = Priority.ALWAYS
                style = "-fx-font-size: 16px;"

                promptTraceContextMenu(this@FormattedPromptResultArea, trace) {
                    item("Copy output to clipboard") {
                        action {
                            clipboard.setContent(mapOf(
                                DataFormat.HTML to selectionHtml.value,
                                DataFormat.PLAIN_TEXT to plainText()
                            ))
                        }
                    }
                }
            }
            this += htmlArea
        }
    }

    init {
        root
        selectionFormatted.onChange {
            htmlArea.children.clear()
            htmlArea.children.addAll(it?.toFxNodes() ?: listOf())
        }
    }

}
