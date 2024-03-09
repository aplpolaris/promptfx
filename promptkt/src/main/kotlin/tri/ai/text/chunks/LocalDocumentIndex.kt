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
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/** Index of documents and document sections designed for serialization. */
class LocalDocumentIndex {
    var documents: List<LocalDocumentInfo> = mutableListOf()

    companion object {
        fun loadFrom(indexFile: File): LocalDocumentIndex =
            MAPPER.readValue(indexFile)

        fun saveTo(index: LocalDocumentIndex, indexFile: File) {
            MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(indexFile, index)
        }

        private val MAPPER = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
    }
}

/** Serializable version of [TextDocument]. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class LocalDocumentInfo(
    val metadata: TextMetadata,
    val attributes: DocumentAttributes = mutableMapOf(),
    val sections: List<TextSectionInfo> = listOf()
) {
    constructor(doc: TextDocument, sections: List<TextSection>) : this(doc.metadata, doc.attributes, sections.map { TextSectionInfo(it) })
}

/** Serializable version of [TextSection]. */
class TextSectionInfo(
    var first: Int = 0,
    var last: Int = 0,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    var attributes: DocumentAttributes = mutableMapOf()
) {
    constructor(section: TextSection) : this() {
        first = section.first
        last = section.last
        attributes = section.attributes
    }
}
