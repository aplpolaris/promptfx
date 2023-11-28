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

import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.SpeechResponseFormat.Companion.Aac
import com.aallam.openai.api.audio.SpeechResponseFormat.Companion.Flac
import com.aallam.openai.api.audio.SpeechResponseFormat.Companion.Mp3
import com.aallam.openai.api.audio.SpeechResponseFormat.Companion.Opus
import com.aallam.openai.api.audio.Voice.Companion.Alloy
import com.aallam.openai.api.audio.Voice.Companion.Echo
import com.aallam.openai.api.audio.Voice.Companion.Fable
import com.aallam.openai.api.audio.Voice.Companion.Nova
import com.aallam.openai.api.audio.Voice.Companion.Onyx
import com.aallam.openai.api.audio.Voice.Companion.Shimmer
import com.aallam.openai.api.model.ModelId
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Button
import javafx.scene.layout.Priority
import javafx.scene.media.Media
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer
import tornadofx.*
import tri.ai.openai.OpenAiModels
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskResult
import tri.ai.pips.AiTaskResult.Companion.result
import tri.promptfx.AiTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

import java.io.IOException




/** Plugin for the [AudioSpeechView]. */
class AudioSpeechApiPlugin : NavigableWorkspaceViewImpl<AudioSpeechView>("Audio", "Text-to-Speech", AudioSpeechView::class)

/** View for OpenAI API's [TTS](https://platform.openai.com/docs/api-reference/audio/createSpeech) endpoint. */
class AudioSpeechView : AiTaskView("Text-to-Speech", "Provide text to generate speech.") {

    private val TTS_MODELS = OpenAiModels.ttsModels()
    private val TTS_VOICES = listOf(Alloy, Echo, Fable, Nova, Onyx, Shimmer)
    private val AUDIO_FORMATS = listOf(Mp3, Aac, Flac, Opus)

    private var player: MediaPlayer? = null
    lateinit var playButton: Button
    private val file = SimpleObjectProperty<ByteArray?>(null)

    private val input = SimpleStringProperty("")
    private val model = SimpleStringProperty(TTS_MODELS.first())
    private val voice = SimpleObjectProperty(TTS_VOICES.first())
    private val format = SimpleObjectProperty(AUDIO_FORMATS.first())
    private val audioSpeed = SimpleDoubleProperty(1.0)

    init {
        input {
            addInputTextArea(input)
        }
        parameters("Audio Model") {
            field("Model") {
                combobox(model, TTS_MODELS)
            }
            field("Voice") {
                combobox(voice, TTS_VOICES)
            }
            field("Response Format") {
                combobox(format, AUDIO_FORMATS)
            }
            field("Speed") {
                slider(0.25..4.0) {
                    valueProperty().bindBidirectional(audioSpeed)
                }
                label(audioSpeed.asString())
            }
        }
        output {
            playButton = button("Play") {
                enableWhen(file.isNotNull)
                graphic = FontAwesomeIconView(FontAwesomeIcon.PLAY).apply { glyphSize = 28 }
                hgrow = Priority.ALWAYS
                maxWidth = Double.MAX_VALUE
                style { fontSize = 28.px }
                action { playButtonPress() }
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        return when {
            input.value.isNullOrBlank() ->
                AiTaskResult.invalidRequest("No input provided")
            else -> controller.openAiPlugin.client.client.speech(
                SpeechRequest(
                    model = ModelId(model.value),
                    input = input.value,
                    voice = voice.value,
                    responseFormat = format.value,
                    speed = audioSpeed.value
                )
            )
            .also { controller.updateUsage() }
            .let {
                file.set(it)
                result(it, model.value)
            }
        }.asPipelineResult()
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
        try {
            val media = loadAudio(file.value!!)
            playButton.text = "Stop"
            playButton.graphic = FontAwesomeIconView(FontAwesomeIcon.STOP).apply { glyphSize = 28 }
            player = MediaPlayer(media).apply {
                onEndOfMedia = Runnable {
                    playButton.text = "Play"
                    playButton.graphic = FontAwesomeIconView(FontAwesomeIcon.PLAY).apply { glyphSize = 28 }
                    player = null
                }
                play()
            }
        } catch (x: MediaException) {
            error("Error playing audio", "Error playing audio: ${x.message}")
        }
    }

    @Throws(IOException::class)
    private fun loadAudio(audioBytes: ByteArray): Media {
        val tempFile = File.createTempFile("audio", null)
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use {
            it.write(audioBytes)
        }
        return Media(tempFile.toURI().toString())
    }
}
