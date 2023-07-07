package tri.promptfx

import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.Alert
import javafx.scene.image.Image
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import tornadofx.alert
import tri.ai.embedding.EmbeddingDocument
import java.awt.Desktop
import java.awt.image.BufferedImage
import java.io.File

/** A cache of document thumbnails. */
object DocumentUtils {

    private val thumbnailCache = mutableMapOf<String, Image>()

    /**
     * Browses to a given document.
     * TODO - this is a quick way to guess the original extension, but needs to be made more robust
     */
    fun browseToDocument(doc: EmbeddingDocument) {
        val file1 = File(doc.path)
        val fileList = listOf("pdf", "doc", "docx").map {
            File(file1.parentFile, file1.nameWithoutExtension + ".$it")
        } + file1
        fileList.firstOrNull { it.exists() }?.let {
            Desktop.getDesktop().open(it)
        } ?: run {
            alert(Alert.AlertType.ERROR, "File not found", "Could not find file ${doc.path}")
        }
    }

    /**
     * Generate a thumbnail for the document if it doesn't exist.
     * TODO - this may take a while, so make it a delayed event
     */
    fun documentThumbnail(doc: EmbeddingDocument): Image? {
        if (doc.path in thumbnailCache)
            return thumbnailCache[doc.path]

        val file1 = File(doc.path)
        val pdfFile = File(file1.parentFile, file1.nameWithoutExtension + ".pdf")
        if (!pdfFile.exists())
            return null

        val thumb = pdfThumbnail(pdfFile, 300)
        if (thumb != null)
            thumbnailCache[doc.path] = thumb

        return thumb
    }

    private fun pdfThumbnail(file: File, thumbnailSize: Int): Image? {
        if (!file.exists()) {
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