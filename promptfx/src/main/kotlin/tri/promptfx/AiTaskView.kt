package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Side
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextInputControl
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.input.Dragboard
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextCompletion
import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingService
import tri.ai.pips.*
import tri.ai.prompt.trace.*
import tri.util.ui.graphic
import java.lang.Exception

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

/**
 * A view that executes a task and displays the result. Provides placeholders for input, output, and parameters.
 * Provides a general framework for testing out API calls with basic user input/output.
 */
abstract class AiTaskView(title: String, instruction: String, showInput: Boolean = true): View(title) {

    lateinit var inputPane: VBox
    lateinit var outputPane: VBox
    lateinit var buttonBar: HBox
    lateinit var runButton: Button
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
            drawer(side = Side.RIGHT) {
                item("Parameters", expanded = false) {
                    scrollpane {
                        isFitToWidth = true
                        isFitToHeight = true
                        style = "-fx-background-color: transparent; -fx-background-insets: 0"
                        parameterForm = form {
                            children.onChange {
                                this@item.expanded = true
                            }
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
        runButton = button("Run") {
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
    fun input(spacing: Number? = null, padding: Number? = null, vgrow: Priority? = null, op: VBox.() -> Unit) {
        with (inputPane) {
            op()
            if (spacing != null) this.spacing = spacing.toDouble()
            if (padding != null) this.padding = insets(padding.toDouble())
            if (vgrow != null) this.vgrow = vgrow
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

    /** Adds default model parameters (model, temperature, tokens) to the view. */
    fun addDefaultTextCompletionParameters(common: ModelParameters) {
        parameters("Text Completion Model") {
            field("Model") {
                combobox(controller.completionEngine, TextPlugin.textCompletionModels())
            }
            with (common) {
                temperature()
                maxTokens()
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

    /** Adds a place for users to drop an image to the input area of the view. */
    fun addInputImageArea(property: SimpleObjectProperty<Image>) {
        input {
            imageview(property) {
                vgrow = Priority.ALWAYS
                isPreserveRatio = true
                fitWidth = 400.0
                fitHeight = 400.0
                isSmooth = true
                isCache = true
                isPickOnBounds = true
                style {
                    backgroundColor += c("#f0f0f0")
                }
                setOnDragOver {
                    if (it.dragboard.hasImage() || it.dragboard.hasImageFile()) {
                        it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                    }
                    it.consume()
                }
                setOnDragDropped {
                    if (it.dragboard.hasImage()) {
                        property.set(it.dragboard.image)
                    } else if (it.dragboard.hasImageFile()) {
                        property.set(Image(it.dragboard.files.first().toURI().toString()))
                    }
                    it.isDropCompleted = true
                    it.consume()
                }
            }
        }
    }

    private fun Dragboard.hasImageFile() = files.isNotEmpty() && files.first().extension in listOf("png", "jpg", "jpeg")

    /** Adds a default output area to the view. By default, updates with text result of the task. */
    fun addOutputTextArea() {
        val result = PromptResultArea()
        output {
            add(result.root)
        }
        onCompleted {
            val r = it.finalResult
            if (r is AiPromptTrace) {
                result.setFinalResult(r)
            } else {
                // TODO - views should return a prompt trace object wherever possible
                val trace = AiPromptTrace(
                    AiPromptInfo(""),
                    AiPromptModelInfo(completionEngine.modelId),
                    AiPromptExecInfo(),
                    AiPromptOutputInfo(r.toString())
                )
                result.setFinalResult(trace)
            }
        }
    }

    /** Hide the parameters view. */
    fun hideParameters() {
        root.right.hide()
    }

    /** Hide the run button. */
    fun hideRunButton() {
        buttonBar.hide()
    }

    //endregion

    /** Gets the input area of the view. By default this finds the first [TextArea] in the input pane. */
    open fun inputArea(): TextInputControl? = inputPane.children.filterIsInstance<TextArea>().firstOrNull()

    /** Gets the output area of the view. By default, this finds the first node in the output pane. */
    open fun outputArea(): EventTarget? = outputPane.children.firstOrNull()?.let {
        if (it is ScrollPane) it.content else it
    }

    /** Processes whatever input user has provided. */
    abstract suspend fun processUserInput(): AiPipelineResult

    /** Adds a hook to be called when the task has completed. */
    fun onCompleted(op: (AiPipelineResult) -> Unit) {
        onCompleted.add(op)
    }

    /** Called when the task has completed. */
    private fun taskCompleted(message: AiPipelineResult) = onCompleted.forEach { it(message) }

    /** Executes task on a background thread and updates progress info. */
    internal fun runTask() {
        val task = executeTask {
            processUserInput()
        }
        task.ui {
            val errors = it.results.values.mapNotNull { it.error }
            if (errors.isNotEmpty()) {
                // show error message to user
                alert(Alert.AlertType.ERROR, "There was an error executing the task.", errors.first().message)
            } else {
                taskCompleted(it)
            }
            controller.updateUsage()
            progress.taskCompleted(title)
        }
        progress.taskStarted(task, title)
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
