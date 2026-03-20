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
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import tri.ai.core.MPartType
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChatMessage

/**
 * Sealed class hierarchy for individual AI task outputs.
 *
 * Use the typed subclasses [Text], [ChatMessage], [MultimodalMessage], or [Other] to represent
 * different kinds of task output, or use the backward-compatible factory function [invoke] to
 * create an appropriately-typed output from named parameters.
 *
 * Each subtype exposes backward-compatible nullable properties ([text], [message],
 * [multimodalMessage], [other]) so that existing code using these properties compiles unchanged;
 * properties not applicable to a given subtype return `null`.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = AiOutput.Text::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = AiOutput.Text::class, name = "text"),
    JsonSubTypes.Type(value = AiOutput.ChatMessage::class, name = "message"),
    JsonSubTypes.Type(value = AiOutput.MultimodalMessage::class, name = "multimodal"),
    JsonSubTypes.Type(value = AiOutput.Other::class, name = "other"),
)
sealed class AiOutput {

    /** Text content of this output, or `null` if this is not a [Text] output. */
    open val text: String? get() = null

    /** Chat message of this output, or `null` if this is not a [ChatMessage] output. */
    open val message: TextChatMessage? get() = null

    /** Multimodal message of this output, or `null` if this is not a [MultimodalMessage] output. */
    open val multimodalMessage: MultimodalChatMessage? get() = null

    /**
     * Arbitrary object of this output, or `null` if this is not an [Other] output.
     * Not serialized to JSON.
     */
    @get:JsonIgnore
    open val other: Any? get() = null

    /**
     * Finds text content where possible in the output.
     * Throws an error if no text content is found and [ifNone] is null.
     */
    abstract fun textContent(ifNone: String? = null): String

    /**
     * Finds image content based on message part type, or `null` if there is no image content.
     */
    abstract fun imageContent(): String?

    /**
     * Gets whichever typed content is provided by this output.
     */
    abstract fun content(): Any

    // -------------------------------------------------------------------------
    // Sealed subtypes
    // -------------------------------------------------------------------------

    /** A plain-text AI output. */
    data class Text(override val text: String) : AiOutput() {
        override fun textContent(ifNone: String?) = text
        override fun imageContent(): String? = null
        override fun content(): Any = text
        override fun toString() = text
    }

    /** A chat-message AI output (e.g. from a TextChat model). */
    data class ChatMessage(override val message: TextChatMessage) : AiOutput() {
        override fun textContent(ifNone: String?) = message.content
            ?: ifNone
            ?: error("No text content available in output: $this")
        override fun imageContent(): String? = null
        override fun content(): Any = message
        override fun toString() = message.content ?: "(no message content)"
    }

    /** A multimodal-message AI output (e.g. from a vision or image-generation model). */
    data class MultimodalMessage(override val multimodalMessage: MultimodalChatMessage) : AiOutput() {
        override fun textContent(ifNone: String?) =
            multimodalMessage.content?.firstNotNullOfOrNull { it.text }
                ?: ifNone
                ?: error("No text content available in output: $this")
        override fun imageContent(): String? =
            multimodalMessage.content?.firstOrNull { it.partType == MPartType.IMAGE }?.inlineData
        override fun content(): Any = multimodalMessage
        override fun toString() = textContent(ifNone = "(no text in multimodal output)")
    }

    /**
     * An arbitrary-object AI output. The [other] value is **not serialized** to JSON.
     * This subtype is used for non-text, non-message outputs such as embeddings or structured data.
     * [toString] and [textContent] are exception-safe: if [other]'s own [toString] throws, a
     * type-name fallback is returned instead of propagating the exception.
     *
     * **Serialization note:** When a trace containing an [Other] output is persisted (JSON/YAML) and
     * later reloaded, the output will deserialize as an empty `Other` instance — the [other] value is
     * silently lost. Callers that need durable storage of arbitrary outputs must extract and persist
     * [other] separately before the trace is written to disk.
     *
     * A future fix could use a custom `@JsonSerialize`/`@JsonDeserialize` pair that attempts
     * `Jackson.writeValueAsString(other)` and stores the result as a `rawJson: String?` property,
     * falling back to `null` for non-serializable types. Deserialization would then restore as
     * `JsonNode` rather than the original type, which is sufficient for many read-only use cases.
     */
    data class Other(@get:JsonIgnore override val other: Any) : AiOutput() {
        private val fallback get() = "Other(${other::class.simpleName})"
        override fun textContent(ifNone: String?) = runCatching { other.toString() }
            .getOrElse { ifNone ?: fallback }
        override fun imageContent(): String? = null
        override fun content(): Any = other
        override fun toString() = runCatching { other.toString() }.getOrElse { fallback }
    }

}
