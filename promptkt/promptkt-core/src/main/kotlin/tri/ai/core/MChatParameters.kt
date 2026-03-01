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
package tri.ai.core

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
data class MChatVariation(
    val seed: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null
) {
    companion object {
        fun temp(temp: Double?) = MChatVariation(temperature = temp)
    }
}

/** Model parameters related to tool use. */
class MChatTools(
    val toolChoice: MToolChoice = MToolChoice.AUTO,
    val tools: List<MTool>
)

@Serializable(with = MToolChoiceSerializer::class)
sealed interface MToolChoice {
    @JvmInline
    @Serializable
    value class Mode(val value: String) : MToolChoice

    @Serializable
    data class Named(
        @SerialName("type") val type: MToolType,
        @SerialName("function") val function: MFunctionToolChoice
    ) : MToolChoice

    companion object {
        /** Represents the `auto` mode. */
        val AUTO: MToolChoice = Mode("AUTO")

        /** Represents the `none` mode. */
        val NONE: MToolChoice = Mode("NONE")

        /** Specifies a function for the model to call **/
        fun function(name: String): MToolChoice =
            Named(type = MToolType.FUNCTION, function = MFunctionToolChoice(name = name))
    }
}

@JvmInline
@Serializable
value class MToolType(val value: String) {
    companion object {
        val FUNCTION = MToolType("function")
    }
}

@Serializable
data class MFunctionToolChoice(val name: String)

internal class MToolChoiceSerializer : JsonContentPolymorphicSerializer<MToolChoice>(MToolChoice::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MToolChoice> {
        return when (element) {
            is JsonPrimitive -> MToolChoice.Mode.serializer()
            is JsonObject -> MToolChoice.Named.serializer()
            else -> throw UnsupportedOperationException("Unsupported JSON element: $element")
        }
    }
}

class MTool(
    val name: String,
    val description: String,
    val jsonSchema: String
)

/** Reference to a function to execute. */
@Serializable
class MToolCall(
    val id: String,
    val name: String,
    val argumentsAsJson: String
)

enum class MResponseFormat {
    JSON,
    TEXT
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Serializable
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
