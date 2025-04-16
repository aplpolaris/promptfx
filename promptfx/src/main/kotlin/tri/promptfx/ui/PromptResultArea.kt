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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.value.ObservableValue
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
 * Text area displaying one or more prompt results, with associated formatting.
 * Adjusts font size, adds ability to copy/save output to a file.
 */
class PromptResultArea : Fragment("Prompt Result Area") {

    val model = PromptResultAreaModel()

    override val root = vbox {
        vgrow = Priority.ALWAYS
        addtoolbar(model)
        textarea(model.resultText) {
            promptText = "Prompt output will be shown here"
            isEditable = false
            isWrapText = true
            font = Font("Segoe UI Emoji", 18.0)
            vgrow = Priority.ALWAYS

            promptTraceContextMenu(model.trace) {
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
                    enableWhen(model.containsCode)
                    action { copyCode() }
                }
                item("Browse to PlantUML diagram") {
                    enableWhen(model.containsPlantUml)
                    action { browseToPlantUml() }
                }
                item("Show PlantUML diagram") {
                    enableWhen(model.containsPlantUml)
                    action { showPlantUmlPopup() }
                }
            }
        }
    }

    fun setFinalResult(finalResult: AiPromptTraceSupport<out Any?>) {
        model.setFinalResult(finalResult, currentWindow)
    }

    //region DETECTED CODE ACTIONS

    private fun copyCode() {
        val code = model.resultText.value.substringAfter("```").substringAfter("\n").substringBefore("```").trim()
        clipboard.putString(code)
    }

    private fun browseToPlantUml() {
        val url = plantUmlUrlText(model.plantUmlText, type="uml")
        println(url)
        hostServices.showDocument(url)
    }

    private fun showPlantUmlPopup() {
        val url = plantUmlUrlText(model.plantUmlText, type="png")
        val image = Image(url)
        if (!image.isError && !image.isBackgroundLoading && image.width > 0.0 && image.height > 0.0)
            showImageDialog(image)
        else
            error("Error loading PlantUML diagram.")
    }

    //endregion

}

//region UI HELPERS

/** Adds a toolbar that appears when there are multiple results presented. */
fun EventTarget.addtoolbar(model: PromptResultAreaModel) {
    toolbar {
        visibleWhen(model.multiResult.or(model.multiTrace))
        managedWhen(model.multiResult.or(model.multiTrace))
        hbox {
            visibleWhen(model.multiTrace)
            managedWhen(model.multiTrace)
            label("Trace")
            button("", FontAwesomeIcon.ANGLE_DOUBLE_LEFT.graphic) {
                enableWhen(model.traceIndex.greaterThan(0))
                action { model.traceIndex.set(model.traceIndex.value - 1) }
            }
            button("", FontAwesomeIcon.ANGLE_DOUBLE_RIGHT.graphic) {
                enableWhen(model.traceIndex.lessThan(model.traces.sizeProperty.subtract(1)))
                action { model.traceIndex.set(model.traceIndex.value + 1) }
            }
        }
        hbox {
            visibleWhen(model.multiResult)
            managedWhen(model.multiResult)
            label("Result")
            button("", FontAwesomeIcon.ANGLE_LEFT.graphic) {
                enableWhen(model.resultIndex.greaterThan(0))
                action { model.resultIndex.set(model.resultIndex.value - 1) }
            }
            button("", FontAwesomeIcon.ANGLE_RIGHT.graphic) {
                enableWhen(model.resultIndex.lessThan(model.results.sizeProperty.subtract(1)))
                action { model.resultIndex.set(model.resultIndex.value + 1) }
            }
        }
    }
}

/** Set up a context menu with a given prompt trace object. */
fun EventTarget.promptTraceContextMenu(trace: ObservableValue<AiPromptTraceSupport<*>?>, op: ContextMenu.() -> Unit = {}) {
    val value = trace.value ?: return
    lazyContextmenu {
        item("Details...") {
            enableWhen { trace.booleanBinding { it != null } }
            action {
                find<PromptTraceDetailsUi>().apply {
                    setTrace(value)
                    openModal()
                }
            }
        }
        item("Try in template view", graphic = FontAwesomeIcon.SEND.graphic) {
            enableWhen(trace.booleanBinding { it?.prompt?.prompt?.isNotBlank() == true })
            action {
                find<PromptFxWorkspace>().launchTemplateView(value)
            }
        }
        item("Open in prompt history view", graphic = FontAwesomeIcon.SEARCH.graphic) {
            enableWhen(trace.booleanBinding { it?.prompt?.prompt?.isNotBlank() == true })
            action {
                find<PromptFxWorkspace>().launchHistoryView(value)
            }
        }
        buildsendresultmenu(value, find<PromptFxWorkspace>())
        separator()
        op()
    }
}

//endregion