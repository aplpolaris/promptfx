/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.api

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.layout.VBox
import tri.ai.core.JsonRetryConfig
import tri.ai.core.JsonRetryProcessor
import tri.ai.core.MChatParameters
import tri.ai.core.MultimodalChat
import tri.ai.core.MultimodalChatMessage

/**
 * Enhanced chat interface that provides JSON-specific processing capabilities.
 * Implements multi-attempt JSON generation (issue #190) and JSON validation.
 */
class JsonChatProcessor {

    /** Enable JSON retry processing */
    val enableJsonRetry = SimpleBooleanProperty(false)
    
    /** Maximum number of retry attempts */
    val maxAttempts = SimpleIntegerProperty(3)
    
    /** Required JSON type (object, array, or any) */
    val requiredJsonType = SimpleObjectProperty<JsonRetryConfig.JsonType?>(null)
    
    /** Whether to require the entire response to be valid JSON */
    val requireCompleteResponse = SimpleBooleanProperty(false)

    /**
     * Generate chat response with optional JSON retry processing.
     */
    suspend fun generateChatWithJsonRetry(
        model: MultimodalChat,
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters
    ): ChatResult {
        return if (enableJsonRetry.value) {
            val config = JsonRetryConfig(
                maxAttempts = maxAttempts.value,
                requireType = requiredJsonType.value,
                requireCompleteResponse = requireCompleteResponse.value
            )
            
            val result = JsonRetryProcessor.generateWithJsonRetry(config) { attemptNumber ->
                val response = model.chat(messages, parameters).firstValue
                response.content?.first()?.text ?: ""
            }
            
            ChatResult(
                responseText = result.responseText,
                hasValidJson = result.hasValidJson,
                jsonElement = result.jsonElement,
                attempts = result.attemptNumber,
                extractedJson = result.extractedJsonText
            )
        } else {
            val response = model.chat(messages, parameters).firstValue
            val responseText = response.content?.first()?.text ?: ""
            ChatResult(
                responseText = responseText,
                hasValidJson = tri.ai.core.JsonResponseProcessor.containsValidJson(responseText),
                jsonElement = tri.ai.core.JsonResponseProcessor.extractFirstValidJson(responseText),
                attempts = 1,
                extractedJson = tri.ai.core.JsonResponseProcessor.extractFirstJsonText(responseText)
            )
        }
    }

    /**
     * Generate multiple attempts and return all results for comparison.
     */
    suspend fun generateMultipleChatAttempts(
        model: MultimodalChat,
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters,
        attempts: Int = 3
    ): List<ChatResult> {
        val results = JsonRetryProcessor.generateMultipleWithJson(attempts) { attemptNumber ->
            val response = model.chat(messages, parameters).firstValue
            response.content?.first()?.text ?: ""
        }
        
        return results.map { result ->
            ChatResult(
                responseText = result.responseText,
                hasValidJson = result.hasValidJson,
                jsonElement = result.jsonElement,
                attempts = result.attemptNumber,
                extractedJson = result.extractedJsonText
            )
        }
    }

    /**
     * UI configuration for JSON processing options.
     */
    fun configurationUI(): Node {
        val container = VBox()
        container.spacing = 10.0
        
        val enableCheckbox = CheckBox("Enable JSON retry processing")
        enableCheckbox.selectedProperty().bindBidirectional(enableJsonRetry)
        container.children.add(enableCheckbox)
        
        return container
    }
}

/**
 * Result of a chat generation with JSON processing information.
 */
data class ChatResult(
    val responseText: String,
    val hasValidJson: Boolean,
    val jsonElement: kotlinx.serialization.json.JsonElement?,
    val attempts: Int,
    val extractedJson: String?
)