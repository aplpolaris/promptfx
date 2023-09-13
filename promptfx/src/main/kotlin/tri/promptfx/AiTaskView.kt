package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextCompletion
import tri.ai.embedding.EmbeddingService
import tri.ai.pips.AiPipelineResult
import tri.util.ui.graphic
import java.lang.Exception

/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

/**
 * A view that executes a task and displays the result. Provides placeholders for input, output, and parameters.
 * Provides a general framework for testing out API calls with basic user input/output.
 */
abstract class AiTaskView(title: String, instruction: String, showInput: Boolean = true): View(title) {

    lateinit var inputPane: VBox
    lateinit var outputPane: VBox
    lateinit var buttonBar: HBox
    lateinit var parameterForm: Form

    val controller: PromptFxController by inject()
    val progress: AiProgressView by inject()

    val runTooltip = SimpleStringProperty("")
    val onCompleted: MutableList<(AiPipelineResult) -> Unit> = mutableListOf()

    val completionEngine: TextCompletion
        get() = controller.completionEngine.value
    val embeddingService: EmbeddingService
        get() = controller.embeddingService.value

    init {
        disableCreate()
        disableDelete()
        disableRefresh()
        disableSave()
    }

    override val root = borderpane {
        padding = insets(10.0)
        top {
            vbox(10) {
                padding = insets(0, 0, 10, 0)
                label(title) {
                    style = "-fx-font-size: 18px;"
                }
                label(instruction) {
                    style = "-fx-font-size: 14px;"
                }
            }
        }
        center {
            vbox(5) {
                if (showInput) {
                    splitpane {
                        vgrow = Priority.ALWAYS
                        inputPane = vbox { }
                        outputPane = vbox { }
                    }
                } else {
                    outputPane = vbox {
                        vgrow = Priority.ALWAYS
                    }
                }
                buttonBar = buttonBar()
            }
        }
        right {
            hbox {
                isVisible = false
                scrollpane {
                    isFitToWidth = true
                    isFitToHeight = true
                    style = "-fx-background-color: transparent; -fx-background-insets: 0"
                    parameterForm = form {
                        children.onChange {
                            this@hbox.isManaged = true
                            this@hbox.isVisible = true
                        }
                    }
                }
            }
        }
    }

    init {
        addOutputTextArea()
    }

    //region ADDING CONTENT TO VIEW

    /** Adds content to the button bar of the view. */
    private fun EventTarget.buttonBar() = hbox(10) {
        button("Run") {
            runTooltip.onChange {
                if (tooltip == null)
                    tooltip = Tooltip(text)
                else
                    tooltip.text = it
            }
            graphic = FontAwesomeIcon.PLAY.graphic.apply {
                glyphSize = 28
            }
            hgrow = Priority.ALWAYS
            maxWidth = Double.MAX_VALUE
            style { fontSize = 28.px }
            action { runTask() }
        }
    }

    /** Adds content to the input area of the view. */
    fun input(op: VBox.() -> Unit) {
        with (inputPane) {
            op()
        }
    }

    /** Adds content to the output area of the view. */
    fun output(op: VBox.() -> Unit) {
        with (outputPane) {
            op()
        }
    }

    /** Adds content to the input area of the view. */
    fun parameters(text: String, op: Fieldset.() -> Unit) {
        with (parameterForm) {
            fieldset(text) {
                op()
            }
        }
    }

    /** Adds default content to the input area of the view. */
    fun addInputTextArea(property: SimpleStringProperty, op: TextArea.() -> Unit = {}) {
        input {
            textarea(property) {
                vgrow = Priority.ALWAYS
                isWrapText = true
                op()
            }
        }
    }

    /** Adds a default output area to the view. By default, updates with text result of the task. */
    fun addOutputTextArea(): TextArea {
        var result: TextArea? = null
        output {
            result = textarea(SimpleStringProperty("")) {
                promptText = "Prompt output will be shown here"
                isEditable = false
                isWrapText = true
                font = Font("Segoe UI Emoji", 18.0)
                vgrow = Priority.ALWAYS
            }
        }
        onCompleted {
            result!!.text = it.finalResult.toString()
        }
        return result!!
    }

    //endregion

    /** Processes whatever input user has provided. */
    abstract suspend fun processUserInput(): AiPipelineResult

    /** Adds a hook to be called when the task has completed. */
    fun onCompleted(op: (AiPipelineResult) -> Unit) {
        onCompleted.add(op)
    }

    /** Called when the task has completed. */
    private fun taskCompleted(message: AiPipelineResult) = onCompleted.forEach { it(message) }

    /** Executes task on a background thread and updates progress info. */
    private fun runTask() {
        progress.task = executeTask {
            processUserInput()
        } ui {
            val errors = it.results.values.mapNotNull { it.error }
            if (errors.isNotEmpty()) {
                // show error message to user
                alert(Alert.AlertType.ERROR, "There was an error executing the task.", errors.first().message)
            } else {
                taskCompleted(it)
            }
            controller.updateUsage()
        }
    }

    /** Run task on a background thread. */
    private fun executeTask(block: suspend () -> AiPipelineResult) = runAsync {
        runBlocking {
            try {
                block()
            } catch (x: Exception) {
                x.printStackTrace()
                AiPipelineResult.error(x.message ?: "Unknown error", x)
            }
        }
    }

}
