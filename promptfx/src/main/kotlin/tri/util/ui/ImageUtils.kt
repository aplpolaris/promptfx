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