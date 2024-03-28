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

/**
 * A text chunk that is a contiguous section of a larger document (also represented here as a chunk).
 * Tracks first and last character in a text string, inclusive.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class TextChunkInDoc(
    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    val first: Int,
    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    val last: Int
): TextChunk() {

    constructor(range: IntRange): this(range.first, range.last)

    override fun text(doc: TextChunk?) =
        doc!!.text(null).substring(first..last)

    @get:JsonIgnore
    val range
        get() = first..last
    @get: JsonIgnore
    val length
        get() = last - first
}
