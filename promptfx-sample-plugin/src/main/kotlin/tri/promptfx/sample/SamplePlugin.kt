/*-
 * #%L
 * tri.promptfx:promptfx-sample-plugin
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
package tri.promptfx.sample

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.text.FontWeight
import tornadofx.*
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Sample plugin demonstrating NavigableWorkspaceView implementation. */
class SamplePlugin : NavigableWorkspaceViewImpl<SampleView>("Sample", "Hello World", WorkspaceViewAffordance.INPUT_ONLY, SampleView::class)

/** Simple view demonstrating basic functionality. */
class SampleView : View("Sample Plugin Demo") {

    private val inputText = SimpleStringProperty("")
    private val outputText = SimpleStringProperty("Hello, World!")

    override val root = vbox {
        paddingAll = 10.0
        spacing = 10.0

        label("Sample Plugin Demonstration") {
            style {
                fontSize = 16.px
                fontWeight = FontWeight.BOLD
            }
        }

        label("This is a simple demonstration of a custom NavigableWorkspaceView plugin.")

        separator()

        hbox {
            spacing = 10.0
            alignment = Pos.CENTER_LEFT

            label("Enter text:")
            textfield(inputText) {
                prefWidth = 200.0
                action {
                    updateOutput()
                }
            }
            button("Process") {
                action {
                    updateOutput()
                }
            }
        }

        label("Output:")
        textarea(outputText) {
            isEditable = false
            prefRowCount = 5
        }

        separator()

        label("Instructions:") {
            style {
                fontWeight = FontWeight.BOLD
            }
        }
        
        label("1. Enter some text in the input field above")
        label("2. Click 'Process' or press Enter")
        label("3. The output will show a processed version of your input")
        
        separator()
        
        label("Plugin Information:") {
            style {
                fontWeight = FontWeight.BOLD
            }
        }
        
        label("• This plugin demonstrates how to create a custom NavigableWorkspaceView")
        label("• It shows up in the 'Sample' category in the PromptFx UI")
        label("• The plugin can be packaged as a JAR and loaded dynamically")
    }

    private fun updateOutput() {
        val input = inputText.value ?: ""
        if (input.isNotBlank()) {
            outputText.value = "Processed: ${input.uppercase()}\n" +
                    "Length: ${input.length} characters\n" +
                    "Words: ${input.split(Regex("\\s+")).size}\n" +
                    "Reversed: ${input.reversed()}"
        } else {
            outputText.value = "Hello, World!\n\nEnter some text above to see it processed."
        }
    }

    init {
        updateOutput()
    }
}