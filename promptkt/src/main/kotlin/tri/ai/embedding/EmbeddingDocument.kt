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
package tri.ai.embedding

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.ai.text.chunks.BrowsableSource
import java.net.URI

/** A document with a list of sections. */
class EmbeddingDocument(val uri: URI) {
    /** The sections of the document. */
    val sections: MutableList<EmbeddingSection> = mutableListOf()

    /** Get browsable source of this document. */
    @get:JsonIgnore
    val browsable = BrowsableSource(uri)

    /** The raw text of the section. */
    fun readText(section: EmbeddingSection) =
        readText().substring(section.start, section.end)

    /** The raw text of the document. */
    fun readText() = browsable.file?.readText()!!
}

