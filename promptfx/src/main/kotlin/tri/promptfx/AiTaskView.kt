package tri.promptfx

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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.core.TextCompletion
import tri.ai.embedding.EmbeddingService
import tri.ai.pips.*
import tri.ai.prompt.trace.*
import tri.ai.prompt.trace.AiImageTrace
import tri.ai.text.docs.FormattedPromptTraceResult
import tri.promptfx.ui.FormattedPromptResultArea
import tri.promptfx.ui.PromptResultArea
import tri.util.ui.graphic
import java.io.File
import java.lang.Exception

/**
 * A view that executes a task and displays the result. Provides placeholders for input, output, and parameters.
 * Provides a general framework for testing out API calls with basic user input/output.
 */
abstract class AiTaskView(title: String, val instruction: String, val showInput: Boolean = true): View(title) {

    lateinit var inputPane: VBox
    lateinit var outputPane: VBox
    lateinit var buttonBar: HBox
    lateinit var runButton: Button
    lateinit var parameterForm: Form

    val controller: PromptFxController by inject()
    val progress: AiProgressView by inject()
    val resultArea = PromptResultArea()
    val formattedResultArea = FormattedPromptResultArea()

    val runTooltip = SimpleStringProperty("")
    val onCompleted: MutableList<(AiPipelineResult<*>) -> Unit> = mutableListOf()

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
            shortcut("Ctrl+R")
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
                combobox(controller.completionEngine, PromptFxModels.textCompletionModels())
            }
            with (common) {
                temperature()
                maxTokens()
                numResponses()
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
        with (inputPane) {
            // add menu to paste, if system clipboard has an image
            contextmenu {
                item("") // placeholder item, required to get context menu to show later
                onShowing = EventHandler {
                    items.clear()
                    items.add(MenuItem("Paste Image").apply {
                        val clipboard = controller.clipboard
                        isDisable = !clipboard.hasImage() && !clipboard.hasImageFile() && !clipboard.hasImageFilePath()
                        action {
                            pasteImageFromClipboard(property)
                        }
                    })
                }
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
        input {
            toolbar {
                button("", FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN)) {
                    action {
                        promptFxFileChooser(
                            "Select an image to describe",
                            arrayOf(PromptFxConfig.FF_IMAGE, PromptFxConfig.FF_ALL),
                            dirKey = PromptFxConfig.DIR_KEY_IMAGE
                        ) {
                            it.firstOrNull()?.let {
                                property.set(Image(it.toURI().toString()))
                            }
                        }
                    }
                }
                button("", FontAwesomeIconView(FontAwesomeIcon.CLIPBOARD)) {
                    enableWhenImageOnClipboard()
                    action { pasteImageFromClipboard(property) }
                }
            }
            imageview(property) {
                vgrow = Priority.ALWAYS
                isPreserveRatio = true
                //region dynamically adjust preview size
                val imageWidth = property.doubleBinding { it?.width ?: 400.0 }
                val imageHeight = property.doubleBinding { it?.height ?: 400.0 }
                val bestWidth = inputPane.widthProperty().doubleBinding(imageWidth) { minOf(it!!.toDouble(), imageWidth.doubleValue()) }
                val bestHeight = inputPane.heightProperty().doubleBinding(imageHeight) { minOf(it!!.toDouble(), imageHeight.doubleValue()) }
                fitWidthProperty().bind(bestWidth)
                fitHeightProperty().bind(bestHeight)
                minWidth = 200.0
                minHeight = 200.0
                //endregion
                isSmooth = true
                isCache = true
                isPickOnBounds = true
                style {
                    backgroundColor += c("#f0f0f0")
                }
            }
        }
    }

    private fun Node.enableWhenImageOnClipboard() {
        runAsync {
            while (true) {
                runLater {
                    isDisable = !controller.clipboard.hasImage() && !controller.clipboard.hasImageFile() && !controller.clipboard.hasImageFilePath()
                }
                Thread.sleep(1000)
            }
        }
    }

    private fun pasteImageFromClipboard(property: SimpleObjectProperty<Image>) {
        if (clipboard.hasImage()) {
            property.set(clipboard.image)
        } else if (clipboard.hasImageFile()) {
            property.set(Image(clipboard.files.first().toURI().toString()))
        } else if (clipboard.hasImageFilePath()) {
            val file = clipboard.fileFromPlainTextContent()!!
            property.set(Image(file.toURI().toString()))
        }
    }

    /** Adds a default output area to the view. By default, updates with text result of the task. */
    fun addOutputTextArea() {
        output {
            add(resultArea.root)
        }
        onCompleted {
            setFinalResult(it.finalResult)
        }
    }

    protected fun setFinalResult(result: AiPromptTraceSupport<*>) {
        when (result) {
            is AiPromptTrace<*> ->
                resultArea.setFinalResult(result)
            is FormattedPromptTraceResult ->
                formattedResultArea.setFinalResult(result)
            is AiImageTrace -> {
                // ignore - this is handled by view
            }
            else -> throw IllegalStateException("Unexpected result type: $result")
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
    abstract suspend fun processUserInput(): AiPipelineResult<*>

    /** Adds a hook to be called when the task has completed. */
    fun onCompleted(op: (AiPipelineResult<*>) -> Unit) {
        onCompleted.add(op)
    }

    /** Called when the task has completed. */
    private fun taskCompleted(message: AiPipelineResult<*>) = onCompleted.forEach { it(message) }

    /** Executes task on a background thread and updates progress info. */
    internal open fun runTask(op: suspend () -> AiPipelineResult<*> = ::processUserInput) {
        val task = executeTask {
            op()
        }
        task.ui {
            val errors = it.interimResults.values.mapNotNull { it.exec.error }
            if (errors.size == 1) {
                // show error message to user
                alert(Alert.AlertType.ERROR, "There was an error executing the task.", errors.first(), owner = currentWindow)
            } else if (errors.isNotEmpty()) {
                // show multiple errors to user
                alert(Alert.AlertType.ERROR, "There were multiple errors executing the task.", errors.joinToString("\n"), owner = currentWindow)
            } else {
                taskCompleted(it)
            }
            controller.updateUsage()
            controller.addPromptTraces(title, it)
            progress.taskCompleted(title)
        }
        progress.taskStarted(task, title)
    }

    /** Run task on a background thread. */
    private fun executeTask(block: suspend () -> AiPipelineResult<*>) = runAsync {
        runBlocking {
            try {
                block()
            } catch (x: Exception) {
                x.printStackTrace()
                AiPipelineResult(AiPromptTrace.error<Any>(null, x.message ?: "Unknown error", x), mapOf())
            }
        }
    }

}

/** Check if clipboard has an image. */
fun Clipboard.hasImageFile() =
    files.isNotEmpty() && files.first().extension.lowercase() in listOf("png", "jpg", "jpeg")

/** Check if clipboard has an image file path. */
fun Clipboard.hasImageFilePath() =
    fileFromPlainTextContent()?.let { it.extension.lowercase() in listOf("png", "jpg", "jpeg") } ?: false

/** Get a file from the clipboard content, if there is a string that appears as a file. */
fun Clipboard.fileFromPlainTextContent(): File? =
    (getContent(DataFormat.PLAIN_TEXT) as? String)?.let {
        it.removePrefix("\"").removeSuffix("\"").substringBefore(",")
            .let { File(it) }
    }
