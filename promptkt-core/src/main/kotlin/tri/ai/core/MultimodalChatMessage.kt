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
package tri.ai.core

/** Generic representation of a multimodal chat message. */
data class MultimodalChatMessage(
    /** Role for what generated the message. */
    val role: MChatRole,
    /** Content of the message. */
    val content: List<MChatMessagePart>? = null,
    /** List of tool calls (typically populated automatically by AI) to invoke for more information. */
    val toolCalls: List<MToolCall>? = null,
    /** Unique id of a tool call (typically populated by the code wrapping up the tool result for the next chat call), used to link a tool call request to the result. */
    val toolCallId: String? = null
) {
    companion object {
        /** Chat message with just text. */
        fun text(role: MChatRole, text: String) = MultimodalChatMessage(
            role,
            listOf(MChatMessagePart(text = text))
        )
        /** Chat message with a tool result. */
        fun tool(result: String, toolId: String) = MultimodalChatMessage(
            MChatRole.Tool,
            listOf(MChatMessagePart(text = result)),
            toolCallId = toolId
        )
    }
}

//region BUILDERS

/** Build a [MultimodalChatMessage] from a builder. */
fun chatMessage(role: MChatRole? = null, block: MChatMessageBuilder.() -> Unit) =
    MChatMessageBuilder().apply(block).also {
        if (role != null) it.role = role
    }.build()

/** Builder object for [MultimodalChatMessage]. */
class MChatMessageBuilder {
    var role = MChatRole.User
    var content = mutableListOf<MChatMessagePart>()
    var params: MChatParameters? = null
    var toolCalls = mutableListOf<MToolCall>()
    var toolCallId: String? = null

    fun role(role: MChatRole) {
        this.role = role
    }
    fun text(text: String) {
        content += MChatMessagePart(MPartType.TEXT, text)
    }
    fun image(imageUrl: String) {
        content += MChatMessagePart(MPartType.IMAGE, inlineData = imageUrl)
    }
    fun content(vararg parts: MChatMessagePart) {
        content += parts.toList()
    }
    fun content(parts: List<MChatMessagePart>) {
        content += parts
    }
    fun parameters(block: MChatParameters.() -> Unit) {
        params = MChatParameters().apply(block)
    }
    fun toolCalls(vararg calls: MToolCall) {
        toolCalls += calls.toList()
    }
    fun toolCalls(calls: List<MToolCall>) {
        toolCalls += calls
    }
    fun toolCallId(id: String?) {
        toolCallId = id
    }

    fun build() = MultimodalChatMessage(role, content, toolCalls, toolCallId)
}

//endregion
