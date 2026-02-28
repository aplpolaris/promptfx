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
package tri.ai.geminisdk

import com.google.genai.types.Candidate
import com.google.genai.types.FunctionCall
import com.google.genai.types.Part
import tri.ai.core.*
import tri.ai.geminisdk.GeminiSdkMultimodalChat.Companion.asString
import tri.ai.prompt.trace.*
import tri.util.info
import kotlin.jvm.optionals.getOrNull

/** Gemini multimodal chat model using the official SDK. */
class GeminiSdkMultimodalChat(
    override val modelId: String,
    private val client: GeminiSdkClient
) : MultimodalChat {

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = parameters.tokens, stop = parameters.stop, requestJson = parameters.responseFormat == MResponseFormat.JSON)
        val t0 = System.currentTimeMillis()

        if ((parameters.numResponses ?: 1) > 1)
            info<GeminiSdkMultimodalChat>("Gemini chat API does not support multiple responses; only the first response will be returned.")

        try {
            val response = client.generateContent(
                modelId = modelId,
                variation = parameters.variation,
                history = messages,
                tools = parameters.tools,
                numResponses = parameters.numResponses ?: 1
            )

            if (response.promptFeedback().isPresent) {
                return AiPromptTrace.invalidRequest(modelInfo, response.promptFeedback().get().toString())
            }

            val candidates = response.candidates().getOrNull() ?: listOf()

            if (candidates.isEmpty()) {
                return AiPromptTrace.error(modelInfo, "Gemini returned no candidates", duration = System.currentTimeMillis() - t0)
            } else {
                val firstCandidate = candidates.first()
                val msg = firstCandidate.fromGeminiCandidate()
                return AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo.multimodalMessage(msg)
                )
            }
        } catch (e: Exception) {
            return AiPromptTrace.error(modelInfo, e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

    override fun close() {
        client.close()
    }

    override fun toString() = "$modelId (Gemini SDK)"

    companion object {
        fun Candidate.fromGeminiCandidate(): MultimodalChatMessage {
            val content = content().get()
            val role = content.role().get().fromGeminiRole()
            val parts = content.parts().get()
            val toolCalls = parts.filter { it.functionCall().isPresent }.map { it.functionCall().get() }
                .map { it.fromGeminiFunctionCall() }
            val contentParts = parts.map { it.fromGeminiPart() }
            return MultimodalChatMessage(role, contentParts, toolCalls = toolCalls)
        }

        fun String.fromGeminiRole() = when (this) {
            "USER","user" -> MChatRole.User
            "MODEL","model" -> MChatRole.Assistant
            else -> error("Invalid Gemini role: $this")
        }

        fun Part.fromGeminiPart() = when {
            text().isPresent -> MChatMessagePart.text(text().get())
            inlineData().isPresent -> MChatMessagePart.image(String(inlineData().get().data().get()))
            functionCall().isPresent -> MChatMessagePart.toolCall(functionCall().get().name().get(), functionCall().get().args().get().mapValues { it.value.toString() })
            functionResponse().isPresent -> MChatMessagePart.toolResponse(functionResponse().get().name().get(), functionResponse().get().response().get().mapValues { it.value.toString() })
            else -> throw UnsupportedOperationException("Unsupported Gemini part: $this")
        }

        fun FunctionCall.fromGeminiFunctionCall() = MToolCall(
            id = "",
            name = name().get(),
            argumentsAsJson = args().get().asString()
        )

        fun Map<String, Any>.asString() = "{" + entries.joinToString(",") { (k, v) -> "\"$k\":\"$v\"" } + "}"

    }

}
