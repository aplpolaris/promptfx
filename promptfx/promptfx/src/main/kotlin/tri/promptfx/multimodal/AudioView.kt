/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.multimodal

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
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
import tri.ai.core.SpeechToTextModel
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.asPipelineResult
import tri.ai.prompt.trace.*
import tri.promptfx.AiTaskView
import tri.promptfx.ModelParameters
import tri.promptfx.PromptFxModels
import tri.util.ui.AudioRecorder
import tri.util.ui.NavigableWorkspaceViewImpl
import java.io.File

/** Plugin for the [AudioView]. */
class AudioApiPlugin : NavigableWorkspaceViewImpl<AudioView>("Multimodal", "Speech-to-Text", type = AudioView::class)

/** View for audio transcription. */
class AudioView : AiTaskView("Speech-to-Text ", "Drop audio file below to transcribe (mp3, mp4, mpeg, mpga, m4a, wav, or webm)") {

    private val AUDIO_MODELS: List<SpeechToTextModel> get() = PromptFxModels.speechToTextModels()

    private val input = SimpleStringProperty("")
    private val file = SimpleObjectProperty<File?>(null)
    private val model = SimpleObjectProperty<SpeechToTextModel>()
    private val common = ModelParameters()

    private var recorder: AudioRecorder? = null
    private var player: MediaPlayer? = null

    lateinit var recordButton: Button
    lateinit var playButton: Button

    init {
        model.set(AUDIO_MODELS.firstOrNull())
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
                combobox(model, AUDIO_MODELS) {
                    cellFormat { text = it.modelDisplayName() }
                }
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

    override suspend fun processUserInput(): AiPipelineResult {
        val f = file.value ?: return AiPromptTrace.invalidRequest(model.value?.modelId ?: "", "No audio file dropped").asPipelineResult()
        val sttModel = model.value ?: return AiPromptTrace.invalidRequest("", "No speech-to-text model selected").asPipelineResult()
        return sttModel.transcribe(f).also { controller.updateUsage() }.asPipelineResult()
    }

}
