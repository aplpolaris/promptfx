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

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableStringValue
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.control.ContextMenu
import javafx.scene.control.TextArea
import javafx.scene.text.TextFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.docs.FormattedPromptTraceResult
import tri.promptfx.docs.FormattedText
import tri.promptfx.docs.toFxNodes
import tri.promptfx.library.TextLibraryInfo

/** General-purpose capability for sending inputs to PromptFx views and getting a result. */
object PromptFxDriver {

    const val IMMERSIVE_VIEW = "Immersive Chat"

    /** Send input to a named view, run, and execute a callback when the result is received. */
    suspend fun PromptFxWorkspace.sendInput(viewName: String, input: String, callback: (FormattedText) -> Unit): FormattedText {
        return if (viewName == IMMERSIVE_VIEW)
            sendImmersiveChatInput(input, callback)
        else
            sendViewInput(viewName, input, callback)
    }

    private suspend fun PromptFxWorkspace.sendImmersiveChatInput(input: String, callback: (FormattedText) -> Unit): FormattedText {
        var view = immersiveChatView

        // allow one retry
        if (view == null) {
            delay(2000)
            view = immersiveChatView
        }

        val result = if (view == null) {
            FormattedText("ImmersiveChatView not available right now.")
        } else {
            val deferred = CompletableDeferred<FormattedText>()
            view.setUserInput(input) { deferred.complete(it) }
            deferred.await()
        }
        callback(result)
        return result
    }

    private suspend fun PromptFxWorkspace.sendViewInput(viewName: String, input: String, callback: (FormattedText) -> Unit): FormattedText {
        val taskView = findTaskView(viewName)
        val inputArea = taskView?.inputArea()
        val outputArea = taskView?.outputArea()

        val result = if (inputArea == null) {
            FormattedText("No view found with name $viewName, or that view does not support input.")
        } else {
            inputArea.text = input
            val result = taskView.processUserInput()
            val nodeResult = (result.finalResult as? FormattedPromptTraceResult)?.text
                ?: result.finalResult as? FormattedText
                ?: FormattedText(result.finalResult.toString())
            Platform.runLater {
                if (outputArea is TextArea) {
                    outputArea.text = nodeResult.toString()
                } else if (outputArea is TextFlow) {
                    outputArea.children.clear()
                    outputArea.children.addAll(nodeResult.toFxNodes())
                }
                taskView.controller.updateUsage()
            }
            nodeResult
        }

        callback(result)
        return result
    }

    /** Apply an input to a task view, bring the view to focus, and execute the task. */
    fun PromptFxWorkspace.setInputAndRun(view: AiTaskView, input: String) {
        view.inputArea()?.text = input
        workspace.dock(view)
        view.runTask()
    }

    /** Apply a collection to a task view. */
    fun UIComponent.loadCollection(view: UIComponent, collection: TextLibraryInfo) {
        (view as? TextLibraryReceiver)?.loadTextLibrary(collection)
        workspace.dock(view)
    }

    /** Show a dialog for testing the PromptFxDriver. */
    fun Component.showDriverDialog() {
        // show dialog getting the view name to target, and the input
        val dialog = find<PromptFxDriverDialog>()
        dialog.openModal(block = true)

        if (dialog.execute)
            runAsync {
                runBlocking {
                    (workspace as PromptFxWorkspace).sendInput(dialog.targetView.value, dialog.input.value) {
                        println("Callback Result: $it")
                    }
                }
            }
    }

}

/** Dialog for testing PromptFxDriver. */
internal class PromptFxDriverDialog: Fragment("PromptFxDriver test dialog") {

    val targetView = SimpleStringProperty("Document Q&A")
    val input = SimpleStringProperty("what are trace diagrams?")
    var execute = false

    override val root = vbox {
        form {
            fieldset {
                field("View Name:") {
                    val keys = listOf(PromptFxDriver.IMMERSIVE_VIEW) + (workspace as PromptFxWorkspace).views.keys.toList()
                    combobox(targetView, keys) {
                        isEditable = true
                    }
                }
                field("Input:") { textarea(input) }
            }
        }
        buttonbar {
            padding = Insets(10.0)
            spacing = 10.0
            button("Go") {
                action {
                    execute = true
                    close()
                }
            }
        }
    }
}

/** Context menu for sending result in an [AiPromptTrace] to a view that accepts a text input. */
fun ContextMenu.buildsendresultmenu(trace: ObservableValue<AiPromptTrace>, workspace: PromptFxWorkspace) {
    buildsendresultmenu(trace.stringBinding { it?.outputInfo?.output }, workspace)
}

/** Context menu for sending a string result to a view that accepts a text input. */
fun ContextMenu.buildsendresultmenu(output: String?, workspace: PromptFxWorkspace) {
    buildsendresultmenu(SimpleStringProperty(output), workspace)
}

/** Context menu for sending result in a string property to a view that accepts a text input. */
fun ContextMenu.buildsendresultmenu(value: ObservableStringValue, workspace: PromptFxWorkspace) {
    menu("Send result to view") {
        disableWhen(value.booleanBinding { it.isNullOrBlank() })

        workspace.viewsWithInputs.forEach { (group, map) ->
            if (map.isNotEmpty()) {
                menu(group) {
                    map.forEach { (_, info) ->
                        val view = workspace.find(info.view) as AiTaskView
                        item(view.title) {
                            action {
                                with (PromptFxDriver) {
                                    workspace.setInputAndRun(view, value.value)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Context menu for sending a chunk collection to another view. */
fun ContextMenu.buildsendcollectionmenu(view: UIComponent, value: ObservableValue<TextLibraryInfo>) {
    menu("Load collection in view") {
        disableWhen(value.booleanBinding { it == null })

        val workspace = view.workspace as PromptFxWorkspace
        workspace.viewsWithCollections.forEach { (group, map) ->
            if (map.isNotEmpty()) {
                menu(group) {
                    map.forEach { (_, info) ->
                        val targetView = workspace.find(info.view) as AiTaskView
                        if (view != targetView) {
                            item(targetView.title) {
                                action {
                                    with(PromptFxDriver) {
                                        view.loadCollection(targetView, value.value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}