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
package tri.promptfx.`fun`

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TextArea
import javafx.scene.paint.Color
import tornadofx.*
import tri.ai.pips.promptPlan
import tri.promptfx.AiPlanTaskView
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [ColorView]. */
class ColorPlugin : NavigableWorkspaceViewImpl<ColorView>("Fun", "Text-to-Color", WorkspaceViewAffordance.INPUT_ONLY, ColorView::class)

/** View to approximate a color based on user text. */
class ColorView : AiPlanTaskView("Colors", "Enter a description of a color or object to generate a color.") {

    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        parameters("Prompt") {
            promptfield(promptId = "example-color", workspace = workspace)
            with (common) {
                numResponses()
            }
        }

        resultArea.trace.onChange {
            updateOutputTextAreaColor(it.toString())
        }
        resultArea.selectionString.onChange {
            updateOutputTextAreaColor(it.toString())
        }
    }

    private fun updateOutputTextAreaColor(color: String) {
        val outputEditor = outputPane.lookup(".text-area") as TextArea
        try {
            val text = color.substringAfter("#").trim()
            val col = Color.web(
                if (text.length == 5 && text.startsWith("FF"))
                    text.substring(2)
                else
                    text
            )
            val fgColor = if (col.brightness > 0.5) "#000000" else "#ffffff"
            // update outputEditor foreground and background colors based on this
            outputEditor.lookup(".content").style = "-fx-background-color: ${col.hex()};"
            outputEditor.style = "-fx-text-fill: $fgColor;"
        } catch (x: IllegalArgumentException) {
            println("Failed to switch color. Result was $color.")
        }
    }

    override fun plan() = completionEngine.promptPlan(
        "example-color",
        input.get(),
        tokenLimit = 6,
        temp = null,
        stop = ";",
        numResponses = common.numResponses.value
    )

    private fun Color.hex() = "#${this.toString().substring(2, 8)}"

}
