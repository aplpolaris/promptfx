/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.embedding

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.File
import java.net.URL

/** A document with a list of sections. */
class EmbeddingDocument(val path: String) {
    /** The sections of the document. */
    val sections: MutableList<EmbeddingSection> = mutableListOf()

    /** Get short name of path. */
    @get:JsonIgnore
    val shortName: String
        get() = File(path).name

    /** Get short name of path without extension. */
    @get:JsonIgnore
    val shortNameWithoutExtension: String
        get() = shortName.substringBeforeLast('.')

    /** Get file. */
    @get:JsonIgnore
    val file: File
        get() {
            val f = File(path)
            return listOf("pdf", "doc", "docx", "txt").map {
                File(f.parentFile, f.nameWithoutExtension + ".$it")
            }.firstOrNull { it.exists() } ?: f
        }

    /** Get URL of path. */
    @get:JsonIgnore
    val url: URL
        get() = file.toURI().toURL()

    /** The raw text of the document. */
    fun readText() = File(path).readText()

    /** The raw text of the section. */
    fun readText(section: EmbeddingSection) = readText().substring(section.start, section.end)
}

/** A section of a document. */
class EmbeddingSection(
    val embedding: List<Double>,
    val start: Int,
    val end: Int
) {
    @get:JsonIgnore
    val length
        get() = end - start
}

/** A section with associated document (not for serialization). */
class EmbeddingSectionInDocument(val doc: EmbeddingDocument, val section: EmbeddingSection) {
    fun readText() = doc.readText(section)
}
