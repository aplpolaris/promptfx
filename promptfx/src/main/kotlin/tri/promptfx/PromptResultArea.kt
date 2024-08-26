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
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.scene.control.ContextMenu
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TXT
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_TXT
import tri.promptfx.ui.PromptTraceDetails
import tri.util.ui.PlantUmlUtils.plantUmlUrlText
import tri.util.ui.graphic
import tri.util.ui.showImageDialog

/**
 * Text area for displaying a prompt result or other output, with support for cycling outputs if multiple.
 * Adjusts font size, adds ability to copy/save output to a file.
 */
class PromptResultArea : Fragment("Prompt Result Area") {

    private val results = observableListOf<String>()
    private val selectedIndex = SimpleIntegerProperty()
    private val selected = selectedIndex.stringBinding(results) { results.getOrNull(it?.toInt() ?: 0) }
    val trace = SimpleObjectProperty<AiPromptTrace>(null)

    private val multiResult = results.sizeProperty.greaterThan(1)
    private val containsCode = selected.booleanBinding { it != null && it.lines().count { it.startsWith("```") } >= 2 }
    private val containsPlantUml = selected.booleanBinding { it != null && (
            it.contains("@startuml") && it.contains("@enduml") ||
            it.contains("@startmindmap") && it.contains("@endmindmap")
        )
    }

    fun setFinalResult(finalResult: AiPromptTrace) {
        val results = finalResult.outputInfo.outputs?.map { it?.toString() ?: "(no result)" } ?: listOf("(no result)")
        this.results.setAll(results)
        selectedIndex.set(0)
        trace.set(finalResult as? AiPromptTrace)
    }

    override val root = vbox {
        vgrow = Priority.ALWAYS
        toolbar {
            visibleWhen(multiResult)
            managedWhen(multiResult)
            button("", FontAwesomeIcon.ARROW_LEFT.graphic) {
                enableWhen(selectedIndex.greaterThan(0))
                action { selectedIndex.set(selectedIndex.value - 1) }
            }
            button("", FontAwesomeIcon.ARROW_RIGHT.graphic) {
                enableWhen(selectedIndex.lessThan(results.sizeProperty.subtract(1)))
                action { selectedIndex.set(selectedIndex.value + 1) }
            }
        }
        textarea(selected) {
            promptText = "Prompt output will be shown here"
            isEditable = false
            isWrapText = true
            font = Font("Segoe UI Emoji", 18.0)
            vgrow = Priority.ALWAYS

            promptTraceContextMenu(this@PromptResultArea, trace) {
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
    }

    //region DETECTED CODE ACTIONS

    private fun copyCode() {
        val code = selected.value.substringAfter("```").substringAfter("\n").substringBefore("```").trim()
        clipboard.putString(code)
    }

    private fun plantUmlText() = if ("@startuml" in selected.value) {
        "@startuml\n" + selected.value.substringAfter("@startuml").substringBefore("@enduml").trim() + "\n@enduml"
    } else {
        "@startmindmap\n" + selected.value.substringAfter("@startmindmap").substringBefore("@endmindmap").trim() + "\n@endmindmap"
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

/** Set up a context menu with a given prompt trace object. */
fun EventTarget.promptTraceContextMenu(component: Component, trace: SimpleObjectProperty<AiPromptTrace>, op: ContextMenu.() -> Unit = {}) =
    lazyContextmenu {
        item("Details...") {
            enableWhen { trace.isNotNull }
            action {
                find<PromptTraceDetails>().apply {
                    setTrace(trace.value)
                    openModal()
                }
            }
        }
        item("Try in template view", graphic = FontAwesomeIcon.SEND.graphic) {
            enableWhen(trace.booleanBinding { it != null && it.promptInfo.prompt.isNotBlank() })
            action {
                (component.workspace as PromptFxWorkspace).launchTemplateView(trace.value)
            }
        }
        buildsendresultmenu(trace, component.workspace as PromptFxWorkspace)
        separator()
        op()
    }