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
import javafx.geometry.Insets
import javafx.scene.control.TextArea
import javafx.scene.text.TextFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.promptfx.docs.FormattedPromptTraceResult
import tri.promptfx.docs.FormattedText
import tri.promptfx.docs.toFxNodes

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
    fun Component.setInputAndRun(view: AiTaskView, input: String) {
        view.inputArea()?.text = input
        workspace.dock(view)
        view.runTask()
    }

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
