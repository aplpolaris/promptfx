package tri.promptfx.`fun`

import javafx.beans.property.SimpleStringProperty
import javafx.scene.paint.Color
import tri.ai.openai.promptPlan
import tri.promptfx.AiPlanTaskView
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl

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

/** Plugin for the [ColorView]. */
class ColorPlugin : NavigableWorkspaceViewImpl<ColorView>("Fun", "Text-to-Color", isScriptable = true, ColorView::class)

/** View to approximate a color based on user text. */
class ColorView : AiPlanTaskView("Colors", "Enter a description of a color or object to generate a color.") {

    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        parameters("Prompt") {
            promptfield(promptId = "example-color", workspace = workspace)
        }
        onCompleted {
            updateOutputTextAreaColor(it.finalResult.toString())
        }
    }

    private fun updateOutputTextAreaColor(result: String) {
        val outputEditor = outputPane.lookup(".text-area") as javafx.scene.control.TextArea
        try {
            val text = result.substringAfter("#").trim()
            val color = Color.web(
                if (text.length == 5 && text.startsWith("FF"))
                    text.substring(2)
                else
                    text
            )
            val fgColor = if (color.brightness > 0.5) "#000000" else "#ffffff"
            // update outputEditor foreground and background colors based on this
            outputEditor.lookup(".content").style = "-fx-background-color: ${color.hex()};"
            outputEditor.style = "-fx-text-fill: $fgColor;"
        } catch (x: IllegalArgumentException) {
            println("Failed to switch color. Result was $result.")
        }
    }

    override fun plan() = completionEngine.promptPlan(
        "example-color",
        input.get(),
        tokenLimit = 6,
        temp = null,
        stop = ";"
    )

    private fun Color.hex() = "#${this.toString().substring(2, 8)}"

}
