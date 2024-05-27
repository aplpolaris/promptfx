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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TXT
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_TXT
import tri.promptfx.PromptFxDriver.setInputAndRun
import tri.promptfx.ui.PromptTraceDetails
import tri.util.ui.PlantUmlUtils.plantUmlUrlText
import tri.util.ui.graphic
import tri.util.ui.showImageDialog


/**
 * Text area for displaying a prompt result or other output. Adjusts font size, adds ability to copy/save output to a file.
 */
class PromptResultArea : Fragment("Prompt Result Area") {

    private val text = SimpleStringProperty("")
    val trace = SimpleObjectProperty<AiPromptTrace>(null)

    private val containsCode = text.booleanBinding { it != null && it.lines().count { it.startsWith("```") } >= 2 }
    private val containsPlantUml = text.booleanBinding { it != null && (
            it.contains("@startuml") && it.contains("@enduml") ||
            it.contains("@startmindmap") && it.contains("@endmindmap")
        )
    }

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
                        setTrace(this@PromptResultArea.trace.value)
                        openModal()
                    }
                }
            }
            item("Try in template view", graphic = FontAwesomeIcon.SEND.graphic) {
                enableWhen(trace.booleanBinding { it != null && it.promptInfo.prompt.isNotBlank() })
                action {
                    (workspace as PromptFxWorkspace).launchTemplateView(trace.value)
                }
            }
            menu("Send result to view") {
                enableWhen(trace.booleanBinding { it != null && !it.outputInfo.output.isNullOrBlank() })
                // add menu items dynamically, when you load the menu
                trace.onChange {
                    items.clear()
                    (workspace as PromptFxWorkspace).viewsWithInputs.forEach { (group, list) ->
                        if (list.isNotEmpty()) {
                            menu(group) {
                                list.forEach {
                                    val view = find(it) as AiTaskView
                                    item(view.title) {
                                        action {
                                            with (PromptFxDriver) {
                                                setInputAndRun(view, trace.value.outputInfo.output!!)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            separator()
            item("Select all") {
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
            separator()
            item("Copy code") {
                enableWhen(containsCode)
                action { copyCode() }
            }
            item("Browse to PlantUML diagram") {
                enableWhen(containsPlantUml)
                action { browseToPlantUml() }
            }
            item("Show PlantUML diagram") {
                enableWhen(containsPlantUml)
                action { showPlantUmlPopup() }
            }
        }
    }

    //region DETECTED CODE ACTIONS

    private fun copyCode() {
        val code = text.value.substringAfter("```").substringAfter("\n").substringBefore("```").trim()
        clipboard.putString(code)
    }

    private fun plantUmlText() = if ("@startuml" in text.value) {
        "@startuml\n" + text.value.substringAfter("@startuml").substringBefore("@enduml").trim() + "\n@enduml"
    } else {
        "@startmindmap\n" + text.value.substringAfter("@startmindmap").substringBefore("@endmindmap").trim() + "\n@endmindmap"
    }

    private fun browseToPlantUml() {
        val url = plantUmlUrlText(plantUmlText(), type="uml")
        println(url)
        hostServices.showDocument(url)
    }

    private fun showPlantUmlPopup() {
        val url = plantUmlUrlText(plantUmlText(), type="png")
        val image = Image(url)
        if (!image.isError && !image.isBackgroundLoading && image.width > 0.0 && image.height > 0.0)
            showImageDialog(image)
        else
            error("Error loading PlantUML diagram.")
    }

    //endregion

}
