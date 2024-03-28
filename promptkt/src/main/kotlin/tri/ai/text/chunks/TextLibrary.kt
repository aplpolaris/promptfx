/*-
 * #%L
 * tri.promptfx:promptkt
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
import java.net.URI
import java.net.URISyntaxException

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
                    val uri = doc.metadata.path
                    if (uri != null) {
                        try {
                            val file = LocalFileManager.fixPath(File(uri), indexFile.parentFile)
                            doc.all = TextChunkRaw(file!!.fileToText(useCache = true))
                        } catch (x: URISyntaxException) {
                            warning<TextLibrary>("Failed to parse URI path syntax for ${doc.metadata}")
                        } catch (x: NullPointerException) {
                            warning<TextLibrary>("Failed to find file for ${doc.metadata}")
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

