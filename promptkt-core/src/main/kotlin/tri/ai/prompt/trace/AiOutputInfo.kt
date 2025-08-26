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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChatMessage

/** Text inference output info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiOutputInfo(
    var outputs: List<AiOutput>
) {
    /** Convert output using a provided function. */
    fun map(transform: (AiOutput) -> AiOutput) =
        AiOutputInfo(outputs.map(transform))

    /** Convert list of output to a single output. */
    fun reduce(transform: (List<AiOutput>) -> AiOutput) =
        AiOutputInfo(listOf(transform(outputs)))

    companion object {
        fun output(output: AiOutput) = AiOutputInfo(listOf(output))
        fun output(outputs: List<AiOutput>) = AiOutputInfo(outputs)

        fun text(text: String) = AiOutputInfo(listOf(AiOutput(text = text)))
        fun text(texts: List<String>) = AiOutputInfo(texts.map { AiOutput(text = it) })

        fun message(message: TextChatMessage) = AiOutputInfo(listOf(AiOutput(message = message)))
        fun messages(messages: List<TextChatMessage>) = AiOutputInfo(messages.map { AiOutput(message = it) })

        fun multimodalMessage(message: MultimodalChatMessage) = AiOutputInfo(listOf(AiOutput(multimodalMessage = message)))
        fun multimodalMessages(messages: List<MultimodalChatMessage>) = AiOutputInfo(messages.map { AiOutput(multimodalMessage = it) })

        /**
         * Accepts any object type, attempting to automatically populate content based on object type.
         * Not recommended for general use.
         */
        fun other(content: Any, allowList: Boolean = false) = when (content) {
            is AiOutput -> output(content)
            is String -> text(content)
            is TextChatMessage -> message(content)
            is MultimodalChatMessage -> multimodalMessage(content)
            is List<*> -> when {
                !allowList -> error("cannot use method `other` for lists since behavior is ambiguous")
                content.all { it is AiOutput } -> output(content.filterIsInstance<AiOutput>())
                content.all { it is String } -> text(content.filterIsInstance<String>())
                content.all { it is TextChatMessage } -> messages(content.filterIsInstance<TextChatMessage>())
                content.all { it is MultimodalChatMessage } -> multimodalMessages(content.filterIsInstance<MultimodalChatMessage>())
                else -> AiOutputInfo(listOf(AiOutput(other = content))) // don't separate list of mixed or nonstandard types
            }
            else -> AiOutputInfo(listOf(AiOutput(other = content)))
        }

    }
}

/** Encapsulates outputs from an AI processing step. */
class AiOutput(
    val text: String? = null,
    val message: TextChatMessage? = null,
    val multimodalMessage: MultimodalChatMessage? = null,
    @get:JsonIgnore
    val other: Any? = null
) {

    override fun toString(): String = textContent(ifNone = other?.toString() ?: "(no output)")

    /** Finds text content where possible in the output. */
    fun textContent(ifNone: String? = null): String = text
        ?: message?.content
        ?: multimodalMessage?.content?.firstNotNullOfOrNull { it.text }
        ?: other?.toString()
        ?: ifNone
        ?: error("No text content available in output: $this")

    /** Gets whichever message content is provided. */
    fun content(): Any = message ?: multimodalMessage ?: text ?: other ?: error("No content available in output: $this")
}