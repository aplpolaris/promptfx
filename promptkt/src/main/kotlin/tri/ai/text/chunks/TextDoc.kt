package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.text.chunks.process.LocalFileManager
import java.io.File
import java.net.URI
import java.time.LocalDate

/**
 * Collection of [TextChunk]s with metadata.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class TextDoc(id: String? = null, _all: TextChunkRaw? = null) {
    /** Metadata. */
    val metadata = TextDocMetadata(id ?: "")
    /** Additional attributes. */
    val attributes: TextAttributes = mutableMapOf()
    /** Text chunks within this book. */
    val chunks = mutableListOf<TextChunk>()

    /** Optional chunk representation of the entire book contents. */
    @get:JsonIgnore
    var all: TextChunkRaw? = _all

    /** Construct a [TextDoc] with a given text string. */
    constructor(id: String, text: String) : this(id, TextChunkRaw(text))

    override fun toString() = metadata.id

    /**
     * Gets a [BrowsableSource] based on the path in the metadata.
     * If [rootDir] is provided, the path is resolved relative to it, if the metadata path file cannot be found.
     */
    fun browsable(rootDir: File?): BrowsableSource? {
        require(rootDir == null || rootDir.isDirectory)
        val path = metadata.path ?: return null
        val uri = URI(path)
        val file: File? = try {
            File(uri).let { if (rootDir != null) LocalFileManager.fixPath(it, rootDir) else it }
        } catch (x: IllegalArgumentException) {
            // URI is not a file
            null
        }?.let { if (it.exists()) it else null }
        return BrowsableSource(file?.toURI() ?: uri)
    }
}

/**
 * Metadata for [TextDoc].
 * The [path] parameter encodes location, preferably as a URI. See [LocalFileManager] for how this is parsed in the case of files.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class TextDocMetadata(
    var id: String,
    var title: String? = null,
    var author: String? = null,
    var date: LocalDate? = null,
    var path: String? = null,
    var relativePath: String? = null,
)