/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.api

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.Button
import tornadofx.*
import tri.ai.core.SpeechToTextModel
import tri.promptfx.PromptFxModels
import java.io.File
import java.net.URI
import java.util.Properties

//region PREFERENCES

/** Singleton for persisting the preferred speech-to-text model across sessions. */
object TranscribePreferences {
    private val PREFS_FILE = File("config/transcribe.properties")
    private const val KEY_MODEL_ID = "stt.model.id"

    private var _modelId: String? = null
    var isTranscribeModelSelected: Boolean = false

    /** The last-used speech-to-text model ID, loaded lazily from disk. */
    var modelId: String?
        get() {
            if (_modelId == null) _modelId = load()
            return _modelId
        }
        set(value) {
            _modelId = value
            save()
        }

    private fun load(): String? {
        if (!PREFS_FILE.exists()) return null
        return runCatching {
            Properties().apply { PREFS_FILE.inputStream().use { load(it) } }.getProperty(KEY_MODEL_ID)?.ifBlank { null }
        }.getOrNull()
    }

    private fun save() {
        runCatching {
            PREFS_FILE.parentFile?.mkdirs()
            Properties().apply {
                _modelId?.let { setProperty(KEY_MODEL_ID, it) }
                PREFS_FILE.outputStream().use { store(it, null) }
            }
        }
    }

    /** Find the stored model from the available models list, or null if not found. */
    fun resolvedModel(): SpeechToTextModel? {
        val id = modelId ?: return null
        return PromptFxModels.speechToTextModels().firstOrNull { it.modelId == id }
    }
}

//endregion

//region DIALOG

/** Dialog for choosing a speech-to-text model. */
class TranscribeModelChooser : Fragment("Choose Speech-to-Text Model") {

    private val models: List<SpeechToTextModel> get() = PromptFxModels.speechToTextModels()
    private val selected = SimpleObjectProperty<SpeechToTextModel>(
        TranscribePreferences.resolvedModel() ?: models.firstOrNull()
    )

    /** Result: the chosen model, or null if cancelled. */
    var result: SpeechToTextModel? = null

    override val root = borderpane {
        prefWidth = 380.0
        center = form {
            padding = insets(16.0)
            fieldset("Speech-to-Text Model") {
                field("Model") {
                    if (models.isEmpty()) {
                        label("No speech-to-text models available.")
                    } else {
                        combobox(selected, models) {
                            cellFormat { text = it.modelDisplayName() }
                            prefWidth = 300.0
                        }
                    }
                }
            }
        }
        bottom = hbox(8.0, Pos.CENTER_RIGHT) {
            padding = insets(8.0, 12.0)
            button("Cancel") {
                action { close() }
            }
            button("Transcribe") {
                isDefaultButton = true
                enableWhen(selected.isNotNull)
                action {
                    result = selected.value
                    TranscribePreferences.modelId = result?.modelId
                    TranscribePreferences.isTranscribeModelSelected = result != null
                    close()
                }
            }
        }
    }
}

//endregion

//region BUTTON BUILDER

/**
 * Adds a "Transcribe" button that:
 * - Is only enabled when [audioUri] is non-null
 * - Picks or prompts for STT model on the first click
 * - Calls [onTranscript] with the transcribed text on success, or [onError] on failure
 */
fun EventTarget.transcribeButton(
    audioUri: () -> URI?,
    isWorking: javafx.beans.value.ObservableValue<Boolean>? = null,
    onTranscript: (String) -> Unit,
    onError: (String) -> Unit
): Button = button("", FontAwesomeIconView(FontAwesomeIcon.FONT)) {
    tooltip("Transcribe audio to text")
    val isTranscribing = SimpleBooleanProperty(false)
    if (isWorking != null) {
        val externalBusy = Bindings.createBooleanBinding({ isWorking.value == true }, isWorking)
        disableProperty().bind(Bindings.or(isTranscribing, externalBusy))
    } else {
        disableWhen(isTranscribing)
    }
    action {
        val uri = audioUri() ?: return@action
        val model = resolveOrChooseSttModel() ?: return@action
        val audioFile = if (uri.scheme == "file") File(uri) else null
        if (audioFile == null) {
            onError("Cannot transcribe: audio must be a local file.")
            return@action
        }
        isTranscribing.set(true)
        // Run transcription in a background thread
        runAsync {
            runCatching { kotlinx.coroutines.runBlocking { model.transcribe(audioFile) } }
        } ui { res ->
            isTranscribing.set(false)
            val tr = res.getOrElse { onError(it.message ?: "Unknown error"); return@ui }
            val text = tr.values?.firstOrNull()?.textContent()
            if (text.isNullOrBlank()) onError("Transcription returned empty text.")
            else onTranscript(text)
        }
    }
}

/** Resolve the STT model from preferences or show chooser dialog; returns null if user cancels or no models are available. */
private fun resolveOrChooseSttModel(): SpeechToTextModel? {
    if (TranscribePreferences.isTranscribeModelSelected) {
        return TranscribePreferences.resolvedModel()
    }
    // Always prompt until a model is selected in the current app run.
    val chooser = find<TranscribeModelChooser>()
    chooser.openModal(block = true)
    return chooser.result
}

//endregion
