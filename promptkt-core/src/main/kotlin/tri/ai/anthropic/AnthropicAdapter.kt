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
package tri.ai.anthropic

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.*
import tri.ai.prompt.trace.*

/** Adapter for Anthropic API client with usage tracking. */
class AnthropicAdapter(val settings: AnthropicApiSettings, _client: HttpClient) {

    var client = _client
        internal set

    companion object {
        private val INSTANCE_SETTINGS = AnthropicApiSettingsBasic()
        val INSTANCE = AnthropicAdapter(INSTANCE_SETTINGS, INSTANCE_SETTINGS.buildClient())
        var apiKey
            get() = INSTANCE_SETTINGS.apiKey
            set(value) {
                INSTANCE_SETTINGS.apiKey = value
                INSTANCE.client = INSTANCE_SETTINGS.buildClient()
            }

        private val MAPPER = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)!!
    }

    /** Anthropic API usage stats. */
    val usage = mutableMapOf<UsageUnit, Int>()

    /** Runs a chat completion request. */
    suspend fun chatCompletion(request: AnthropicChatRequest): AiPromptTrace {
        settings.checkApiKey()

        val t0 = System.currentTimeMillis()
        
        try {
            val response = client.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", settings.apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(MAPPER.writeValueAsString(request))
            }
            
            val responseText = response.body<String>()
            val responseBody = MAPPER.readValue<AnthropicChatResponse>(responseText)
            usage.increment(responseBody.usage)

            val promptInfo = if (request.messages.size == 1 && request.messages.first().content.isNotEmpty()) {
                val firstContent = request.messages.first().content.first()
                if (firstContent is AnthropicContent) {
                    firstContent.text?.let { PromptInfo(it) }
                } else null
            } else null

            return AiPromptTrace(
                promptInfo = promptInfo,
                modelInfo = request.toModelInfo(),
                execInfo = AiExecInfo.durationSince(t0, 
                    queryTokens = responseBody.usage.inputTokens, 
                    responseTokens = responseBody.usage.outputTokens),
                outputInfo = AiOutputInfo.messages(responseBody.content.mapNotNull { content ->
                    when (content) {
                        is AnthropicContent -> when (content.type) {
                            "text" -> content.text?.let { TextChatMessage(MChatRole.Assistant, it) }
                            else -> null
                        }
                        else -> null
                    }
                })
            )
        } catch (e: Exception) {
            return AiPromptTrace.error(AiModelInfo(request.model), "Anthropic API error: ${e.message}", e)
        }
    }

    /** Increment usage map with usage from response. */
    private fun MutableMap<UsageUnit, Int>.increment(usage: AnthropicUsage?) {
        this[UsageUnit.TOKENS] = (this[UsageUnit.TOKENS] ?: 0) + (usage?.let { it.inputTokens + it.outputTokens } ?: 0)
    }

    /** Convert request to model info for tracing. */
    private fun AnthropicChatRequest.toModelInfo() = AiModelInfo.info(model,
        AiModelInfo.MAX_TOKENS to maxTokens,
        AiModelInfo.TEMPERATURE to temperature,
        AiModelInfo.TOP_P to topP,
        AiModelInfo.STOP to stopSequences
    )
}

/** Tracks model usage. */
enum class UsageUnit {
    TOKENS,
    AUDIO_SECONDS,
    IMAGES,
    NONE
}

// Data classes for Anthropic API
data class AnthropicChatRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @JsonProperty("max_tokens") val maxTokens: Int,
    val temperature: Double? = null,
    @JsonProperty("top_p") val topP: Double? = null,
    @JsonProperty("stop_sequences") val stopSequences: List<String>? = null,
    val system: String? = null
)

data class AnthropicMessage(
    val role: String,
    val content: List<Any> // Can be AnthropicContent or AnthropicImageContent
)

open class AnthropicContent(
    val type: String,
    val text: String? = null
)

data class AnthropicChatResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContent>,
    val model: String,
    @JsonProperty("stop_reason") val stopReason: String?,
    @JsonProperty("stop_sequence") val stopSequence: String?,
    val usage: AnthropicUsage
)

data class AnthropicUsage(
    @JsonProperty("input_tokens") val inputTokens: Int,
    @JsonProperty("output_tokens") val outputTokens: Int
)