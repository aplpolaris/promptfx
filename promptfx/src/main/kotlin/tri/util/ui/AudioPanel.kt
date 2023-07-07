package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*
import java.io.File

class AudioPanel: HBox() {

    val recordButton: Button
    val file = SimpleObjectProperty<File?>(null)
    val fileText = SimpleStringProperty("")

    private var recorder: AudioRecorder? = null

    init {
        recordButton = button("Record") {
            graphic = FontAwesomeIconView(FontAwesomeIcon.MICROPHONE).apply { glyphSize = 28 }
            hgrow = Priority.ALWAYS
            maxWidth = Double.MAX_VALUE
            style { fontSize = 28.px }
            action { recordButtonPress() }
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

    private fun dropAudioFile(f: File) {
        file.set(f)
        fileText.value = "Audio file: ${f.name}"
    }

}