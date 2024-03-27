package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.fileToText
import tri.ai.text.chunks.process.LocalTextDocIndex
import tri.util.warning
import java.io.File

/**
 * Collection of [TextDoc]s.
 */
class TextLibrary(_id: String? = null) {
    /** Metadata for the library. */
    val metadata = TextLibraryMetadata().apply {
        id = _id ?: ""
    }
    /** Docs in the library. */
    val docs = mutableListOf<TextDoc>()

    override fun toString() = metadata.id

    companion object {
        fun loadFrom(indexFile: File): TextLibrary =
            MAPPER.readValue<TextLibrary>(indexFile).also {
                it.docs.forEach { doc ->
                    doc.metadata.path?.let {
                        File(it).let { f ->
                            val fixFile = LocalFileManager.fixPath(f, indexFile.parentFile)
                            if (fixFile?.exists() == true) {
                                doc.all = TextChunkRaw(fixFile.fileToText(useCache = true))
                            } else {
                                warning<TextLibrary>("Failed to find file for ${doc.metadata.id}")
                            }
                        }
                    }
                }
            }

        fun saveTo(index: TextLibrary, indexFile: File) {
            MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(indexFile, index)
        }

        internal val MAPPER = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .registerModule(SimpleModule().apply {
                addDeserializer(TextChunk::class.java, TextChunkDeserializer())
            })
    }
}

/**
 * Metadata for a [TextLibrary].
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class TextLibraryMetadata(
    var id: String = ""
)

