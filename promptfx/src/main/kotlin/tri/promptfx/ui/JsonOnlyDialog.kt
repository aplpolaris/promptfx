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
package tri.promptfx.ui

import javafx.scene.control.ContextMenu
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TXT
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.PromptFxConfig.Companion.FF_TXT
import tri.promptfx.promptFxFileChooser

/**
 * Dialog for displaying only JSON content with syntax highlighting and formatting.
 * Implements part of issue #100: JSON post-processing options.
 */
class JsonOnlyDialog : Fragment("JSON Content") {

    val jsonContent: String by param()

    override val root = vbox {
        prefWidth = 800.0
        prefHeight = 600.0

        toolbar {
            button("Copy JSON") {
                action {
                    clipboard.putString(jsonContent)
                }
            }
            button("Save JSON...") {
                action {
                    promptFxFileChooser(
                        dirKey = DIR_KEY_TXT,
                        title = "Save JSON to File",
                        filters = arrayOf(FF_JSON, FF_TXT, FF_ALL),
                        mode = FileChooserMode.Save
                    ) {
                        it.firstOrNull()?.writeText(jsonContent)
                    }
                }
            }
            separator()
            button("Close") {
                action { close() }
            }
        }

        textarea(jsonContent) {
            isEditable = false
            isWrapText = true
            font = Font("Courier New", 14.0)
            vgrow = Priority.ALWAYS

            contextmenu {
                item("Select all") {
                    action { selectAll() }
                }
                item("Copy") {
                    action { copy() }
                }
                separator()
                item("Copy all JSON") {
                    action { 
                        clipboard.putString(jsonContent)
                    }
                }
            }
        }

        hbox {
            spacing = 10.0
            button("Copy JSON") {
                action {
                    clipboard.putString(jsonContent)
                    information("Copied", "JSON content copied to clipboard")
                }
            }
            button("Close") {
                action { close() }
            }
        }
    }
}