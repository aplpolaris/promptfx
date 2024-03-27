package tri.ai.text.chunks

import java.io.File
import java.net.URI

/**
 * Provides information needed to browse to a source.
 * Examples: a snippet of text in a PDF, a file, a website, etc.
 */
class BrowsableSource(val uri: URI) {
    /** The path to the source file. */
    val path = uri.path
    /** Get the source as a file, if it exists. */
    val file = try {
        File(uri).let { if (it.exists()) it else null }
    } catch (x: IllegalArgumentException) {
        null
    }
    /** The short name of the source file. */
    val shortName = path.substringAfterLast('/')
    /** The short name of the source file, without extension. */
    val shortNameWithoutExtension = shortName.substringBeforeLast('.')
}