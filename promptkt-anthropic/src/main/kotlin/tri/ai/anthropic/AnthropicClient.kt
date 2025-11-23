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

import com.anthropic.models.messages.*
import tri.ai.core.TextChatMessage
import tri.ai.core.MChatRole
import tri.ai.core.MChatVariation
import java.io.Closeable

/**
 * General purpose client for the Anthropic API.
 * See https://docs.anthropic.com/claude/reference/
 */
class AnthropicClient : Closeable {

    val settings = AnthropicSettings()
    private val client = settings.client

    /** Returns true if the client is configured with an API key. */
    fun isConfigured() = settings.apiKey.isNotBlank()

    //region CORE API METHODS

    /** List available models. */
    fun listModels() = client.models().list().autoPager().stream().toList()

    /** Create a message with the specified parameters. */
    fun createMessage(params: MessageCreateParams): Message {
        return client.messages().create(params)
    }

    /** Create a message from a simple text prompt. */
    fun createMessage(
        prompt: String,
        modelId: String,
        maxTokens: Long = 1024,
        temperature: Double? = null
    ): Message {
        val paramsBuilder = MessageCreateParams.builder()
            .model(modelId)
            .maxTokens(maxTokens)
            .addUserMessage(prompt)

        if (temperature != null) {
            paramsBuilder.temperature(temperature)
        }

        return createMessage(paramsBuilder.build())
    }

    /** Create a message from a chat history. */
    fun createMessage(
        messages: List<TextChatMessage>,
        modelId: String,
        maxTokens: Long = 1024,
        variation: MChatVariation = MChatVariation()
    ): Message {
        val paramsBuilder = MessageCreateParams.builder()
            .model(modelId)
            .maxTokens(maxTokens)

        // Handle system message separately
        val systemMessage = messages.firstOrNull { it.role == MChatRole.System }
        if (systemMessage != null) {
            paramsBuilder.system(systemMessage.content ?: "")
        }

        // Add user and assistant messages
        messages.filter { it.role != MChatRole.System }.forEach { msg ->
            when (msg.role) {
                MChatRole.User -> paramsBuilder.addUserMessage(msg.content ?: "")
                MChatRole.Assistant -> paramsBuilder.addAssistantMessage(msg.content ?: "")
                else -> {} // Ignore other roles
            }
        }

        // Apply variation parameters
        variation.temperature?.let { paramsBuilder.temperature(it) }
        variation.topP?.let { paramsBuilder.topP(it) }

        return createMessage(paramsBuilder.build())
    }

    //endregion

    override fun close() {
        // The Anthropic SDK manages its own resources
    }

}
