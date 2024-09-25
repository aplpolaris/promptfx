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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.scene.control.ContextMenu
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TXT
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_TXT
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.buildsendresultmenu
import tri.promptfx.promptFxFileChooser
import tri.promptfx.ui.trace.PromptTraceDetailsUi
import tri.util.ui.PlantUmlUtils.plantUmlUrlText
import tri.util.ui.graphic
import tri.util.ui.showImageDialog

/**
 * Text area for displaying a prompt result or other output, with support for cycling outputs if multiple.
 * Adjusts font size, adds ability to copy/save output to a file.
 */
class PromptResultArea : PromptResultAreaSupport("Prompt Result Area") {

    private val containsCode = selectionString.booleanBinding { it != null && it.lines().count { it.startsWith("```") } >= 2 }
    private val containsPlantUml = selectionString.booleanBinding { it != null && (
            it.contains("@startuml") && it.contains("@enduml") ||
            it.contains("@startmindmap") && it.contains("@endmindmap")
        )
    }

    override val root = vbox {
        vgrow = Priority.ALWAYS
        addtoolbar()
        textarea(selectionString) {
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
        val code = selectionString.value.substringAfter("```").substringAfter("\n").substringBefore("```").trim()
        clipboard.putString(code)
    }

    private fun plantUmlText() = if ("@startuml" in selectionString.value) {
        "@startuml\n" + selectionString.value.substringAfter("@startuml").substringBefore("@enduml").trim() + "\n@enduml"
    } else {
        "@startmindmap\n" + selectionString.value.substringAfter("@startmindmap").substringBefore("@endmindmap").trim() + "\n@endmindmap"
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

/** Implementation of a prompt result area with support for displaying a list of results. */
abstract class PromptResultAreaSupport(title: String) : Fragment(title) {

    /** The trace being represented in the result area. */
    val trace = SimpleObjectProperty<AiPromptTraceSupport<*>>(null)
    /** List of results to display in the result area. */
    protected val results = observableListOf<String>()
    /** List of results, as [FormattedText], if present. */
    protected val resultsFormatted = observableListOf<FormattedText>()
    /** Whether there are multiple results to display. */
    private val multiResult = results.sizeProperty.greaterThan(1)

    /** Index of the currently selected result. */
    private val selectionIndex = SimpleIntegerProperty()
    /** String representation of the currently selected result. */
    val selectionString = selectionIndex.stringBinding(results) {
        results.getOrNull(it?.toInt() ?: 0)
    }
    /** Formatted representation of the currently selected result. */
    protected val selectionFormatted = selectionIndex.objectBinding(resultsFormatted) {
        resultsFormatted.getOrNull(it?.toInt() ?: 0)
    }
    /** HTML representation of the currently selected result. */
    protected val selectionHtml = selectionFormatted.stringBinding { it?.toHtml() ?: "<html>(No result)" }

    /** Set the final result to display in the result area. */
    fun setFinalResult(finalResult: AiPromptTraceSupport<*>) {
        val results = finalResult.values?.map { it?.toString() ?: "(no result)" } ?: listOf("(no result)")
        this.results.setAll(results)
        this.resultsFormatted.setAll((finalResult as? FormattedPromptTraceResult)?.formattedOutputs ?: listOf())
        selectionIndex.set(0)
        trace.set(finalResult)
    }

    /** Adds a toolbar that appears when there are multiple results presented. */
    protected fun EventTarget.addtoolbar() {
        toolbar {
            visibleWhen(multiResult)
            managedWhen(multiResult)
            button("", FontAwesomeIcon.ANGLE_DOUBLE_LEFT.graphic) {
                enableWhen(selectionIndex.greaterThan(0))
                action { selectionIndex.set(selectionIndex.value - 1) }
            }
            button("", FontAwesomeIcon.ANGLE_DOUBLE_RIGHT.graphic) {
                enableWhen(selectionIndex.lessThan(results.sizeProperty.subtract(1)))
                action { selectionIndex.set(selectionIndex.value + 1) }
            }
        }
    }

}

/** Set up a context menu with a given prompt trace object. */
fun EventTarget.promptTraceContextMenu(component: Component, trace: SimpleObjectProperty<AiPromptTraceSupport<*>>, op: ContextMenu.() -> Unit = {}) =
    lazyContextmenu {
        item("Details...") {
            enableWhen { trace.isNotNull }
            action {
                find<PromptTraceDetailsUi>().apply {
                    setTrace(trace.value)
                    openModal()
                }
            }
        }
        item("Try in template view", graphic = FontAwesomeIcon.SEND.graphic) {
            enableWhen(trace.booleanBinding { it?.prompt?.prompt?.isNotBlank() == true })
            action {
                (component.workspace as PromptFxWorkspace).launchTemplateView(trace.value)
            }
        }
        buildsendresultmenu(trace, component.workspace as PromptFxWorkspace)
        separator()
        op()
    }