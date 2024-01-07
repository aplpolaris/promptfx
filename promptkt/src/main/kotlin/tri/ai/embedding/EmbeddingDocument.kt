/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
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
package tri.ai.embedding

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

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

    /** The file with the raw text. */
    fun rawTextUrl(rootDir: File): File? {
        val file1 = File(rootDir, path)
        val file2 = File(path)
        return when {
            file1.exists() -> file1
            file2.exists() -> file2
            else -> null
        }
    }

    /** The original file. */
    fun originalUrl(rootDir: File): File? {
        val file = rawTextUrl(rootDir) ?: return null
        return SUPPORTED_EXTENSIONS.map {
            File(file.parentFile, file.nameWithoutExtension + ".$it")
        }.firstOrNull { it.exists() } ?: file
    }

    /** The raw text of the document. */
    fun readText(rootDir: File) = rawTextUrl(rootDir)?.readText() ?: "Unable to locate $path in $rootDir"

    /** The raw text of the section. */
    fun readText(rootDir: File, section: EmbeddingSection) =
        readText(rootDir).substring(section.start, section.end)

    companion object {
        /** Extensions supported by the embedding index, either raw text or with available scrapers. */
        val SUPPORTED_EXTENSIONS = listOf("pdf", "doc", "docx", "txt")
    }
}

