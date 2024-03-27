/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import tri.ai.embedding.EmbeddingDocument
import tri.ai.embedding.EmbeddingIndex
import tri.ai.text.chunks.BrowsableSource
import tri.ai.text.chunks.process.LocalFileManager.PDF
import tri.ai.text.chunks.process.LocalFileManager.originalFile
import tri.ai.text.chunks.process.LocalTextDocIndex
import java.awt.image.BufferedImage
import java.io.File
import kotlin.collections.set

/** A cache of document thumbnails. */
object DocumentUtils {

    private val thumbnailCache = mutableMapOf<String, Image>()

    /**
     * Generate a thumbnail for the document if it doesn't exist.
     * TODO - this may take a while, so make it a delayed event
     */
    fun documentThumbnail(doc: BrowsableSource): Image? {
        if (doc.path in thumbnailCache)
            return thumbnailCache[doc.path]

        val pdfFile = doc.uri?.let {
            try {
                val file = File(it)
                file.originalFile()?.let {
                    if (it.extension.lowercase() == PDF) it else null
                }
            } catch (x: IllegalArgumentException) {
                // not a file URI
                null
            }
        } ?: return null
        val thumb = pdfThumbnail(pdfFile, 300)
        if (thumb != null)
            thumbnailCache[doc.path] = thumb

        return thumb
    }

    private fun pdfThumbnail(file: File, thumbnailSize: Int): Image? {
        if (!file.exists() || file.extension != "pdf") {
            return null
        }
        val document = PDDocument.load(file)
        val renderer = PDFRenderer(document)
        val image = renderer.renderImageWithDPI(0, 96f)
        document.close()
        // scale it to thumbnail size but preserve aspect ratio
        val aspect = image.width.toDouble() / image.height.toDouble()
        val width = if (aspect > 1) thumbnailSize else (thumbnailSize * aspect).toInt()
        val height = if (aspect > 1) (thumbnailSize / aspect).toInt() else thumbnailSize
        val scaledImage = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH)
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        bufferedImage.createGraphics().drawImage(scaledImage, 0, 0, null)
        return SwingFXUtils.toFXImage(bufferedImage, null)
    }

}
