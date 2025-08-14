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
package tri.promptfx.api

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import io.ktor.util.*
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.scene.control.Button
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import tornadofx.*
import tri.ai.gemini.*
import tri.ai.openai.OpenAiModelIndex
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.asPipelineResult
import tri.ai.prompt.trace.*
import tri.util.ui.AudioRecorder
import tri.promptfx.AiTaskView
import tri.promptfx.ModelParameters
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.audioUri
import java.io.File
import java.util.*

/** Plugin for the [AudioView]. */
class AudioApiPlugin : NavigableWorkspaceViewImpl<AudioView>("Audio", "Speech-to-Text", type = AudioView::class)

/** View for audio transcription. */
class AudioView : AiTaskView("Speech-to-Text ", "Drop audio file below to transcribe (mp3, mp4, mpeg, mpga, m4a, wav, or webm)") {

    private val AUDIO_MODELS = OpenAiModelIndex.audioModels() + GeminiModelIndex.audioModels()

    private val input = SimpleStringProperty("")
    private val file = SimpleObjectProperty<File?>(null)
    private val model = SimpleStringProperty(AUDIO_MODELS.first())
    private val common = ModelParameters()

    private var recorder: AudioRecorder? = null
    private var player: MediaPlayer? = null

    lateinit var recordButton: Button
    lateinit var playButton: Button

    init {
        input {
            audioPanel()
            addInputTextArea(input) {
                isEditable = false
            }
            onDragOver = EventHandler {
                if (it.dragboard.hasFiles())
                    it.acceptTransferModes(TransferMode.COPY)
            }
            onDragDropped = EventHandler {
                it.dragboard.files.firstOrNull()?.let { dropAudioFile(it) }
            }
        }
    }

    init {
        parameters("Audio Model") {
            field("Model") {
                combobox(model, AUDIO_MODELS)
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
            }
        }
    }

    private fun EventTarget.audioPanel() = hbox {
        recordButton = button("Record") {
            graphic = FontAwesomeIconView(FontAwesomeIcon.MICROPHONE).apply { glyphSize = 28 }
            hgrow = Priority.ALWAYS
            maxWidth = Double.MAX_VALUE
            style { fontSize = 28.px }
            action { recordButtonPress() }
        }
        playButton = button("Play") {
            enableWhen(file.isNotNull)
            graphic = FontAwesomeIconView(FontAwesomeIcon.PLAY).apply { glyphSize = 28 }
            hgrow = Priority.ALWAYS
            maxWidth = Double.MAX_VALUE
            style { fontSize = 28.px }
            action { playButtonPress() }
        }
    }

    fun recordButtonPress() {
        recorder?.let {
            it.finish()
            it.cancel()
            dropAudioFile(it.tempFile)
            recordButton.text = "Record"
            recordButton.graphic = FontAwesomeIconView(FontAwesomeIcon.MICROPHONE).apply { glyphSize = 28 }
            recorder = null
            return
        }

        recorder = AudioRecorder().apply {
            val thread = Thread(this)
            thread.start()
            recordButton.text = "Stop"
            recordButton.graphic = FontAwesomeIconView(FontAwesomeIcon.STOP).apply { glyphSize = 28 }
        }
    }

    fun playButtonPress() {
        player?.let {
            it.stop()
            playButton.text = "Play"
            playButton.graphic = FontAwesomeIconView(FontAwesomeIcon.PLAY).apply { glyphSize = 28 }
            it.dispose()
            player = null
            return
        }
        playButton.text = "Stop"
        playButton.graphic = FontAwesomeIconView(FontAwesomeIcon.STOP).apply { glyphSize = 28 }
        player = MediaPlayer(Media(file.value!!.toURI().toString())).apply {
            onEndOfMedia = Runnable {
                playButton.text = "Play"
                playButton.graphic = FontAwesomeIconView(FontAwesomeIcon.PLAY).apply { glyphSize = 28 }
                player = null
            }
            play()
        }
    }

    private fun dropAudioFile(f: File) {
        file.set(f)
        input.value = "Audio file: ${f.name}"
    }

    override suspend fun processUserInput(): AiPipelineResult<String> {
        return when (val f = file.value) {
            null -> AiPromptTrace.invalidRequest(model.value, "No audio file dropped")
            else -> processAudio(model.value, f)
        }.asPipelineResult()
    }

    private val GEMINI_PROMPT = "Transcribe this audio"

    private suspend fun processAudio(modelId: String, f: File): AiPromptTrace<String> {
        return when (modelId) {
            in OpenAiModelIndex.audioModels() -> {
                controller.openAiPlugin.client.quickTranscribe(modelId, f)
                    .also { controller.updateUsage() }
            }
            in GeminiModelIndex.audioModels() -> {
                val fileBytes = file.value!!.audioUri("wav")
                val request = GenerateContentRequest(Content(
                    listOf(
                        Part(GEMINI_PROMPT),
                        Part(null, Blob.fromDataUrl(fileBytes))
                    ), ContentRole.user
                ))
                val response = GeminiClient().use {
                    it.generateContent(modelId, request)
                }
                AiPromptTrace(
                    AiPromptInfo(GEMINI_PROMPT),
                    AiModelInfo(modelId),
                    AiExecInfo(),
                    AiOutputInfo.output(response.candidates!![0].content.parts[0].text!!)
                )
            }
            else -> return AiPromptTrace.invalidRequest(modelId, "Unknown audio model")
        }
    }

}
