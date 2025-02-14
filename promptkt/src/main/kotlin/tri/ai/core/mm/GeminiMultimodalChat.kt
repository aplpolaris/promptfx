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
package tri.ai.core.mm

import tri.ai.core.TextChatRole
import tri.ai.gemini.*
import tri.ai.gemini.GeminiClient.Companion.fromGeminiRole
import tri.ai.prompt.trace.*

/** Chat completion with Gemini models. */
class GeminiMultimodalChat(override val modelId: String = GeminiModelIndex.GEMINI_15_FLASH, val client: GeminiClient = GeminiClient.INSTANCE) :
    MultimodalChat {

    override fun toString() = modelId

    override suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): AiPromptTrace<MultimodalChatMessage> {
        val modelInfo = AiModelInfo.info(modelId, tokens = parameters.tokens, stop = parameters.stop, requestJson = parameters.responseFormat == MultimodalResponseFormat.JSON)
        val t0 = System.currentTimeMillis()

        val system = messages.lastOrNull { it.role == TextChatRole.System }?.content
        val nonSystem = messages.filter { it.role != TextChatRole.System }
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

        return response.trace(modelInfo, t0)
    }

    companion object {

        private const val DEFAULT_MAX_TOKENS = 500

        /** Create trace for chat message response, with given model info and start query time. */
        internal fun GenerateContentResponse.trace(modelInfo: AiModelInfo, t0: Long): AiPromptTrace<MultimodalChatMessage> {
            val pf = promptFeedback
            return if (pf?.blockReason != null) {
                val msg = "Gemini blocked response: ${pf.blockReason}"
                AiPromptTrace.error(modelInfo, msg, duration = System.currentTimeMillis() - t0)
            } else if (candidates.isNullOrEmpty()) {
                AiPromptTrace.error(modelInfo, "Gemini returned no candidates", duration = System.currentTimeMillis() - t0)
            } else {
                val firstCandidate = candidates!!.first()
                val role = firstCandidate.content.role.fromGeminiRole()
                val msgs = firstCandidate.content.parts.map { MultimodalChatMessage.text(role, it.text!!) }
                AiPromptTrace(
                    null,
                    modelInfo,
                    AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                    AiOutputInfo(msgs)
                )
            }
        }

        //region CONVERSIONS

        fun GenerateContentResponse.toMultimodalChatMessage(): MultimodalChatMessage =
            candidates!!.first().content.let {
                MultimodalChatMessage(
                    role = it.role.fromGeminiRole(),
                    content = it.parts.map { it.fromGeminiPart() }
                )
            }

        fun Part.fromGeminiPart(): MChatMessagePart =
            MChatMessagePart(
                text = text,
                inlineData = inlineData?.data
            )

        fun MultimodalChatMessage.gemini(): Content {
            return Content(
                role = role.gemini(),
                parts = content.map { it.gemini() }
            )
        }

        fun TextChatRole.gemini(): ContentRole {
            return when (this) {
                TextChatRole.User -> ContentRole.user
                TextChatRole.Assistant -> ContentRole.model
                else -> error("Invalid role: $this")
            }
        }

        fun MChatMessagePart.gemini(): Part {
            return Part(
                text = this.text,
                inlineData = this.inlineData?.let { Blob.fromDataUrl(it) }
            )
        }

        fun MChatParameters.gemini(): GenerationConfig {
            return GenerationConfig(
                stopSequences = stop,
                responseMimeType = if (responseFormat == MultimodalResponseFormat.JSON) MIME_TYPE_JSON else null,
                responseSchema = null,
                candidateCount = numResponses,
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

        fun MChatParameters.geminiTools(): List<Tool>? =
            tools?.tools?.mapNotNull { it.gemini() }

        fun MTool.gemini(): Tool? = null // TODO

        fun MChatParameters.geminiToolConfig(): ToolConfig? = null // TODO

        //endregion

    }

}
