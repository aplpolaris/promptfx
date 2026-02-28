/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChatMessage

/** Encapsulates outputs from an AI processing step. */
class AiOutput(
    val text: String? = null,
    val message: TextChatMessage? = null,
    val multimodalMessage: MultimodalChatMessage? = null,
    @get:JsonIgnore
    val other: Any? = null
) {

    override fun toString(): String = textContent(ifNone = other?.toString() ?: "(no output)")

    /**
     * Finds text content where possible in the output.
     */
    fun textContent(ifNone: String? = null): String = text
        ?: message?.content
        ?: multimodalMessage?.content?.firstNotNullOfOrNull { it.text }
        ?: other?.toString()
        ?: ifNone
        ?: error("No text content available in output: $this")

    /**
     * Finds image content based on message part type, or null if there is no image content.
     */
    fun imageContent(): String? =
        multimodalMessage?.content
            ?.firstOrNull { it.partType == MPartType.IMAGE }
            ?.inlineData

    /**
     * Gets whichever message content is provided.
     */
    fun content(): Any = message ?: multimodalMessage ?: text ?: other ?: error("No content available in output: $this")
}
