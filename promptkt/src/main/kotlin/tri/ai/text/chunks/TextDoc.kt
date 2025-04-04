/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.text.chunks

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.text.chunks.process.LocalFileManager
import tri.ai.text.chunks.process.LocalFileManager.PDF
import java.io.File
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

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

    /** Optional header string associated with the doc (e.g. for CSV files). */
    @get:JsonIgnore
    var dataHeader: String?
        get() = attributes[DATA_HEADER_ATTRIBUTE] as? String
        set(value) { attributes[DATA_HEADER_ATTRIBUTE] = value }

    /** Construct a [TextDoc] with a given text string. */
    constructor(id: String, text: String) : this(id, TextChunkRaw(text))

    override fun toString() = metadata.id

    /**
     * Gets a [BrowsableSource] based on the path in the metadata.
     */
    fun browsable() =
        metadata.path?.let { BrowsableSource(it) }

    fun pdfFile(): File? {
        val browsable = browsable() ?: return null
        val isPdf = browsable.path.substringAfterLast(".") == PDF
        val file = browsable.file ?: return null
        return if (isPdf && file.exists()) file else null
    }

    companion object {
        const val DATA_HEADER_ATTRIBUTE = "data_header"
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
    @Deprecated("Use 'dateTime' instead")
    var date: LocalDate? = null,
    var dateTime: LocalDateTime? = null,
    var path: URI? = null,
    var relativePath: String? = null,
) {
    /**
     * Replace existing values with those provided.
     * Updates [title] and [author] but not other local properties.
     */
    fun replaceAll(values: Map<String, Any>) {
        title = values["title"] as? String
        author = values["author"] as? String
        properties.clear()
        properties.putAll(values.filterKeys { it !in listOf("title", "author") })
    }

    /** Additional attributes. */
    var properties: TextAttributes = mutableMapOf()
}
