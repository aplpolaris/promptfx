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
package tri.promptfx.api

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
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskResult
import tri.ai.openai.audioModels
import tri.util.ui.AudioRecorder
import tri.ai.openai.AUDIO_WHISPER
import tri.promptfx.AiTaskView
import tri.promptfx.CommonParameters
import java.io.File

/** View for the OpenAI API's [Whisper](https://platform.openai.com/docs/api-reference/audio) endpoint. */
class AudioView : AiTaskView("Whisper", "Drop audio file below to transcribe (mp3, mp4, mpeg, mpga, m4a, wav, or webm)") {

    private val modelId = AUDIO_WHISPER

    private val input = SimpleStringProperty("")
    private val file = SimpleObjectProperty<File?>(null)
    private val model = SimpleStringProperty(audioModels[0])
    private val common = CommonParameters()

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
                combobox(model, audioModels)
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
        return when (val f = file.value) {
            null -> AiTaskResult.invalidRequest("No audio file dropped")
            else -> controller.openAiPlugin.client.quickTranscribe(modelId, f)
                .also { controller.updateUsage() }
        }.asPipelineResult()
    }

}
