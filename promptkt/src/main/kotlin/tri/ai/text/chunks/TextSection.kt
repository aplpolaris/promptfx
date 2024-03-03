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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

/** A text chunk that is a contiguous section of a larger document. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class TextSection(
    @get:JsonIgnore
    val doc: TextDocument,
    @get:JsonIgnore
    val range: IntRange
) : TextChunk() {

    constructor(doc: TextDocument) : this(doc, doc.text.indices)

    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    val first
        get() = range.first
    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    val last
        get() = range.last

    @get:JsonIgnore
    override val text
        get() = doc.text.substring(range)
    @get: JsonIgnore
    val length
        get() = range.last - range.first
}
