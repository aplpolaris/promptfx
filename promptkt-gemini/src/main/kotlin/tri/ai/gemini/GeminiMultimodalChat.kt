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
package tri.ai.gemini

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import tri.ai.core.*
import tri.ai.gemini.GeminiClient.Companion.fromGeminiRole
import tri.ai.prompt.trace.*
import tri.util.info

/** Chat completion with Gemini models. */
class GeminiMultimodalChat(override val modelId: String = GeminiModelIndex.GEMINI_15_FLASH, val client: GeminiClient = GeminiClient.INSTANCE) :
    MultimodalChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = parameters.tokens, stop = parameters.stop, requestJson = parameters.responseFormat == MResponseFormat.JSON)
        val t0 = System.currentTimeMillis()

        if ((parameters.numResponses ?: 1) > 1)
            info<GeminiMultimodalChat>("Gemini chat API does not support multiple responses; only the first response will be returned.")

        val system = messages.lastOrNull { it.role == MChatRole.System }?.content
        val nonSystem = messages.filter { it.role != MChatRole.System }
        val request = GenerateContentRequest(
            contents = nonSystem.map { it.gemini() },
            tools = parameters.geminiTools(),
            toolConfig = parameters.geminiToolConfig(),
            safetySettings = null,
            systemInstruction = system?.let { Content(it.map { it.gemini() }, ContentRole.user) },
            generationConfig = parameters.gemini(),
            cachedContent = null
        )
        val response = client.generateContent(modelId, request)

        return if (response.promptFeedback != null)
            AiPromptTrace.invalidRequest(modelInfo, response.promptFeedback.toString())
        else
            response.trace(modelInfo, t0)
    }

    companion object {

        private const val DEFAULT_MAX_TOKENS = 500

        /** Create trace for chat message response, with given model info and start query time. */
        internal fun GenerateContentResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace {
            val pf = promptFeedback
            return if (pf?.blockReason != null) {
                val msg = "Gemini blocked response: ${pf.blockReason}"
                AiPromptTrace.error(modelInfo, msg, duration = System.currentTimeMillis() - t0)
            } else if (candidates.isNullOrEmpty()) {
                AiPromptTrace.error(modelInfo, "Gemini returned no candidates", duration = System.currentTimeMillis() - t0)
            } else {
                val firstCandidate = candidates!!.first()
                val msg = firstCandidate.fromGeminiCandidate()
                AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo.multimodalMessage(msg)
                )
            }
        }

        //region CONVERSIONS

        fun GenerateContentResponse.toMultimodalChatMessage(): MultimodalChatMessage =
            candidates!!.first().content.let {
                MultimodalChatMessage(
                    role = it.role.fromGeminiRole(),
                    content = it.parts.map { it.fromGemini() }
                )
            }

        fun Candidate.fromGeminiCandidate(): MultimodalChatMessage {
            val role = content.role.fromGeminiRole()
            val functionCall = content.parts.filter { it.functionCall != null }.map { it.functionCall!! }
            val parts = content.parts.filter { it.text != null || it.inlineData != null }.map { it.fromGemini() }
            return MultimodalChatMessage(role, parts, functionCall.map { it.fromGemini() })
        }

        fun Part.fromGemini(): MChatMessagePart = when {
            text != null -> MChatMessagePart.text(text)
            inlineData != null -> MChatMessagePart.image(inlineData.data)
            functionCall != null -> MChatMessagePart.toolCall(functionCall.name, functionCall.args)
            functionResponse != null -> MChatMessagePart.toolResponse(functionResponse.name, functionResponse.response)
            else -> throw UnsupportedOperationException("Unsupported Gemini part: $this")
        }

        fun MultimodalChatMessage.gemini(): Content {
            return if (!toolCalls.isNullOrEmpty()) {
                val toolCallsList = toolCalls!! // explicit cast
                val args = toolCallsList.first().argumentsAsJson
                val toolCallArgs = Json.decodeFromString<Map<String, String>>(args)
                Content(
                    role = role.gemini(),
                    parts = listOf(MChatMessagePart.toolCall(toolCallsList.first().name, toolCallArgs).gemini())
                )
            } else if (role == MChatRole.Tool) {
                Content(
                    role = role.gemini(),
                    parts = listOf(MChatMessagePart.toolResponse(toolCallId!!, mapOf("result" to content!!.first().text!!)).gemini())
                )
            } else {
                Content(
                    role = role.gemini(),
                    parts = content?.map { it.gemini() } ?: emptyList()
                )
            }
        }

        fun MChatRole.gemini(): ContentRole? {
            return when (this) {
                MChatRole.User -> ContentRole.user
                MChatRole.Assistant -> ContentRole.model
                MChatRole.Tool -> null
                else -> error("Invalid role: $this")
            }
        }

        fun MChatMessagePart.gemini(): Part = when (partType) {
            MPartType.TEXT -> Part(text = text)
            MPartType.IMAGE -> Part(inlineData = Blob.fromDataUrl(inlineData!!))
            MPartType.TOOL_CALL -> Part(functionCall = FunctionCall(name = functionName!!, args = functionArgs!!))
            MPartType.TOOL_RESPONSE -> Part(functionResponse = FunctionResponse(name = functionName!!, response = functionArgs!!))
        }

        fun FunctionCall.fromGemini() = MToolCall(
            id = "",
            name = name,
            argumentsAsJson = args.asString()
        )

        fun Map<String, String>.asString() = "{" + entries.joinToString(",") { (k, v) -> "\"$k\":\"$v\"" } + "}"

        fun MChatParameters.gemini(): GenerationConfig {
            return GenerationConfig(
                stopSequences = stop,
                responseMimeType = responseFormat.gemini(),
                responseSchema = null,
                candidateCount = numResponses, // TODO - as of 3/18/2025, there is an API error for counts >1
                maxOutputTokens = tokens ?: DEFAULT_MAX_TOKENS,
                temperature = variation.temperature,
                topP = variation.topP,
                topK = variation.topK,
                presencePenalty = variation.presencePenalty,
                frequencyPenalty = variation.frequencyPenalty,
                responseLogprobs = null,
                logprobs = null
            )
        }

        fun MChatParameters.geminiTools(): List<Tool>? {
            val funcs = tools?.tools?.map { it.gemini() }
            return if (funcs.isNullOrEmpty()) {
                null
            } else {
                listOf(Tool(funcs))
            }
        }

        fun MResponseFormat.gemini() = when (this) {
            MResponseFormat.TEXT -> MIME_TYPE_TEXT
            MResponseFormat.JSON -> MIME_TYPE_JSON
        }

        fun MTool.gemini() = FunctionDeclaration(name, description, jsonSchema.geminiSchema())

        fun String.geminiSchema() = try {
            Json.decodeFromString<Schema>(this)
        } catch (x: SerializationException) {
            error("Invalid JSON schema: $this")
        }

        fun MChatParameters.geminiToolConfig(): ToolConfig? = null // TODO

        //endregion

    }

}
