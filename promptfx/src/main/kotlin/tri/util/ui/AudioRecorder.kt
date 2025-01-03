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
package tri.util.ui

import javafx.concurrent.Task
import java.io.File
import javax.sound.sampled.*

/** Utility class for recording audio. */
class AudioRecorder: Task<Void>() {

    var tempFile: File = File.createTempFile("audio", ".wav").also {
        it.deleteOnExit()
    }

    private var audioLine: TargetDataLine? = null
    private val audioFormat = AudioFormat(44100f, 16, 2, true, true)
    private val fileType = AudioFileFormat.Type.WAVE

    @Throws(LineUnavailableException::class)
    override fun call(): Void? {
        val audioInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        if (!AudioSystem.isLineSupported(audioInfo)) {
            throw UnsupportedOperationException("Audio line not supported")
        }
        audioLine = (AudioSystem.getLine(audioInfo) as TargetDataLine).apply {
            open(audioFormat)
            start()
        }
        println("Recording started...")
        AudioSystem.write(AudioInputStream(audioLine), fileType, tempFile)
        return null
    }

    fun finish() {
        audioLine?.let {
            it.stop()
            it.close()
        }
        println("Recording stopped.")
    }

}
