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

/** Model parameters for multimodal chat. */
class MChatParameters(
    /** Parameters for varying output. */
    val variation: MChatVariation = MChatVariation(),
    /** Parameters for tool use. */
    val tools: MChatTools? = null,
    /** Parameters for token limit. */
    val tokens: Int? = 1000,
    /** Parameters for stopping criteria. */
    val stop: List<String>? = null,
    /** Parameters for response format. */
    val responseFormat: MResponseFormat = MResponseFormat.TEXT,
    /** Parameters for number of responses. */
    val numResponses: Int? = null
)

/** Model parameters related to likelihood, variation, and probabilities. */
class MChatVariation(
    val seed: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null
)

/** Model parameters related to tool use. */
class MChatTools(
    val toolChoice: MToolChoice = MToolChoice.AUTO,
    val tools: List<MTool>
)

enum class MToolChoice {
    AUTO,
    NONE
}

class MTool(
    val name: String,
    val description: String,
    val jsonSchema: String
)

/** Reference to a function to execute. */
class MToolCall(
    val id: String,
    val name: String,
    val argumentsAsJson: String
)

enum class MResponseFormat {
    JSON,
    TEXT
}

data class MChatMessagePart(
    val partType: MPartType = MPartType.TEXT,
    val text: String? = null,
    // TODO - support for multiple types of inline data
    val inlineData: String? = null,
    val functionName: String? = null,
    val functionArgs: Map<String, String>? = null
) {
    init {
        require(if (partType == MPartType.TEXT) text != null else true) { "Text must be provided for text part type." }
        require(if (partType == MPartType.IMAGE) inlineData != null else true) { "Inline data must be provided for image part type." }
        require(if (partType == MPartType.TOOL_CALL) functionName != null && functionArgs != null else true) { "Function name and arguments must be provided for tool call part type." }
        require(if (partType == MPartType.TOOL_RESPONSE) functionName != null && functionArgs != null else true) { "Function name and arguments must be provided for tool response part type." }
    }

    companion object {
        fun text(text: String) = MChatMessagePart(MPartType.TEXT, text)
        fun image(inlineData: String) = MChatMessagePart(MPartType.IMAGE, inlineData = inlineData)
        fun toolCall(name: String, args: Map<String, String>) = MChatMessagePart(MPartType.TOOL_CALL, functionName = name, functionArgs = args)
        fun toolResponse(name: String, response: Map<String, String>) = MChatMessagePart(MPartType.TOOL_RESPONSE, functionName = name, functionArgs = response)
    }
}

enum class MPartType {
    TEXT,
    IMAGE,
    TOOL_CALL,
    TOOL_RESPONSE
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
