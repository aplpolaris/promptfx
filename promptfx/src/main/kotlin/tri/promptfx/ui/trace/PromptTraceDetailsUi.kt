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
package tri.promptfx.ui.trace

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.Priority
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer
import tornadofx.*
import tri.ai.openai.OpenAiModelIndex
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.*
import tri.promptfx.tools.PromptTraceHistoryView
import tri.util.ui.graphic
import tri.util.ui.loadAudio

/** View showing all details of a prompt trace. */
class PromptTraceDetailsUi : Fragment("Prompt Trace") {

    var trace = SimpleObjectProperty<AiPromptTraceSupport<*>>()

    val prompt = SimpleStringProperty("")
    val promptParams = SimpleObjectProperty<Map<String, Any>>(null)
    val model = SimpleStringProperty("")
    val modelParams = SimpleObjectProperty<Map<String, Any>>(null)
    val exec = SimpleObjectProperty<AiExecInfo>(null)
    val result = SimpleObjectProperty<Any>("")

    lateinit var paramsField: Fieldset

    private val thumbnailSize = SimpleDoubleProperty(128.0)
    lateinit var playButton: Button
    private var player: MediaPlayer? = null

    fun setTrace(trace: AiPromptTraceSupport<*>) {
        this.trace.set(trace)
        prompt.value = trace.prompt?.prompt
        promptParams.value = trace.prompt?.promptParams
        model.value = trace.model?.modelId
        modelParams.value = trace.model?.modelParams
        exec.value = trace.exec
        val outputs = trace.output?.outputs ?: listOf("No result")
        if (outputs.size == 1)
            result.value = outputs[0] ?: "No result"
        else
            result.value = trace.output?.outputs?.joinToString("\n\n") ?: "No result"
    }

    override val root = vbox {
        toolbar {
            // add button to close dialog and open trace in template view
            button("Open in template view", graphic = FontAwesomeIcon.SEND.graphic) {
                enableWhen(trace.isNotNull.and(prompt.isNotBlank()))
                tooltip("Copy this prompt to the Prompt Template view under Tools and open that view.")
                action {
                    if (currentWindow != workspace.currentWindow)
                        close()
                    find<PromptFxWorkspace>().launchTemplateView(trace.value!!)
                }
            }
            // add button to close dialog and open trace in template view
            button("Open in history view", graphic = FontAwesomeIcon.SEARCH.graphic) {
                val isHistoryDocked = workspace.dockedComponentProperty.booleanBinding { it is PromptTraceHistoryView }
                enableWhen(trace.isNotNull.and(exec.isNotNull).and(isHistoryDocked.not()))
                tooltip("Open this prompt in the prompt history view (if available).")
                action {
                    if (currentWindow != workspace.currentWindow)
                        close()
                    find<PromptFxWorkspace>().launchHistoryView(trace.value!!)
                }
            }
        }
        scrollpane {
            prefViewportHeight = 800.0
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            isFitToWidth = true
            form {
                fieldset("Input") {
                    field("Model") {
                        text(model)
                    }
                    field("Model Params") {
                        text(modelParams.stringBinding { it.pretty() })
                    }
                    field("Prompt") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(prompt) {
                            wrappingWidth = 400.0
                        }
                    }
                }
                paramsField = fieldset("Prompt Parameters")
                fieldset("Result") {
                    field("Execution") {
                        text(exec.stringBinding { it.pretty() })
                    }
                    field("Result") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        val box = hbox {
                            text("TBD")
                        }
                        result.onChange {
                            box.children.clear()
                            if (result.value.toString()
                                    .startsWith("https:") && model.value in PromptFxModels.imageModels()
                                    .map { it.modelId }
                            ) {
                                val imageUrl = result.value.toString()
                                box.imageview(imageUrl) {
                                    fitWidthProperty().bind(thumbnailSize)
                                    fitHeightProperty().bind(thumbnailSize)
                                    isPreserveRatio = true
                                    tooltip { graphic = vbox {
                                        val text = text(prompt) {
                                            style = "-fx-fill: white;"
                                        }
                                        val image = imageview(imageUrl)
                                        text.wrappingWidthProperty().bind(image.image.widthProperty())
                                    } }
                                }
                            } else if (result.value is ByteArray && model.value in OpenAiModelIndex.ttsModels()) { // TODO general support for audio models
                                playButton = box.button("Play", graphic = FontAwesomeIcon.PLAY.graphic) {
                                    action { playButtonPress(result.value as ByteArray) }
                                }
                            } else {
                                box.text(result.value.toString()) {
                                    wrappingWidth = 400.0
                                }
                            }
                        }
                    }
                }
            }
            updateParamsField()
            promptParams.onChange { updateParamsField() }
        }
    }

    private fun updateParamsField() {
        with (paramsField) {
            children.removeAll(paramsField.children.drop(1))
            promptParams.value?.let { params ->
                params.entries.forEach { (k, v) ->
                    field(k) {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(v.truncated) {
                            tooltip(v.toString())
                            wrappingWidth = 400.0
                        }
                    }
                }
            }
        }
    }

    private fun Map<String, Any?>?.pretty() =
        this?.entries?.joinToString(", ") { (k, v) -> "$k: ${v.truncated}" } ?: ""
    private fun AiExecInfo?.pretty() = this?.let {
        mapOf<String, Any?>(
            "error" to it.error,
            "query_tokens" to it.queryTokens,
            "response_tokens" to it.responseTokens,
            "duration" to it.responseTimeMillis?.let { "${it}ms" }
        ).entries.filter { it.value != null }
            .joinToString(", ") { (k, v) -> "$k: $v" }
    }

    private val Any?.truncated
        get() = toString().let { if (it.length > 400) it.substring(0, 397) + "..." else it }

    fun playButtonPress(audioBytes: ByteArray) {
        player?.let {
            it.stop()
            playButton.text = "Play"
            playButton.graphic = FontAwesomeIcon.PLAY.graphic
            player = null
            it.dispose()
            return
        }
        try {
            val media = loadAudio(audioBytes)
            playButton.text = "Stop"
            playButton.graphic = FontAwesomeIcon.STOP.graphic
            player = MediaPlayer(media).apply {
                onEndOfMedia = Runnable {
                    playButton.text = "Play"
                    playButton.graphic = FontAwesomeIcon.PLAY.graphic
                    player = null
                }
                play()
            }
        } catch (x: MediaException) {
            error("Error playing audio", "Error playing audio: ${x.message}")
        }
    }
}
