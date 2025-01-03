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

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

/** Encode image as base64 string. */
fun Image.base64(formatName: String = "png"): String {
    val bufferedImage = SwingFXUtils.fromFXImage(this, null)
    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, formatName, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.getEncoder().encodeToString(byteArray)
}

/** Encode image within URL. */
fun Image.toUri(formatName: String = "png"): String = "data:image/$formatName;base64,${base64(formatName)}"
