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

import javafx.scene.media.Media
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/** Encode audio within URL. */
fun File.audioUri(formatName: String = "wav"): String = "data:audio/$formatName;base64,${Base64.getEncoder().encodeToString(readBytes())}"

/** Load audio data encoded as [ByteArray] as a playable [Media] object. */
@Throws(IOException::class)
fun loadAudio(audioBytes: ByteArray): Media {
    val tempFile = File.createTempFile("audio", null)
    tempFile.deleteOnExit()
    FileOutputStream(tempFile).use {
        it.write(audioBytes)
    }
    return Media(tempFile.toURI().toString())
}