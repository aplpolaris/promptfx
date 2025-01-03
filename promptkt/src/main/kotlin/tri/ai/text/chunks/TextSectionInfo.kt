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

import com.fasterxml.jackson.annotation.JsonInclude

/** Serializable version of [TextChunkInDoc]. */
class TextSectionInfo(
    var first: Int = 0,
    var last: Int = 0,
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    var attributes: TextAttributes = mutableMapOf()
) {
    constructor(section: TextChunkInDoc) : this() {
        first = section.first
        last = section.last
        attributes = section.attributes
    }
}
