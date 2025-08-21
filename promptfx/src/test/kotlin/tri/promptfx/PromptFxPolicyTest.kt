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
package tri.promptfx

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import tri.ai.core.*
import tri.ai.embedding.EmbeddingService
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.text.chunks.TextChunk

class PromptFxPolicyTest {

    /** Test policy with mock models for testing default selection logic. */
    private val testPolicy = object : PromptFxPolicy() {
        override fun modelInfo() = emptyList<ModelInfo>()
        
        override fun embeddingModels() = listOf(
            MockEmbeddingService("embedding-1")
        )
        
        override fun textCompletionModels() = listOf(
            MockCompletionChat("chat-completion-1"),  // This is a chat-based completion model
            MockCompletion("pure-completion-1")       // This is a pure completion model
        )
        
        override fun chatModels() = listOf(
            MockChat("chat-1")
        )
        
        override fun multimodalModels() = emptyList<MultimodalChat>()
        override fun visionLanguageModels() = emptyList<VisionLanguageChat>()
        override fun imageModels() = emptyList<ImageGenerator>()
        override fun supportsView(simpleName: String) = true
        override val isShowBanner = false
        override val isShowUsage = false
        override val isShowApiKeyButton = false
        override val bar = PromptFxPolicyBar("Test", javafx.scene.paint.Color.GRAY, javafx.scene.paint.Color.WHITE)
    }

    @Test
    fun testTextCompletionModelDefaultPrefersChatModels() {
        val defaultModel = testPolicy.textCompletionModelDefault()
        
        // Should prefer chat-based completion models over pure completion models
        assertTrue(defaultModel.javaClass.simpleName.contains("Chat"), 
            "Expected default text completion model to be chat-based, but got: ${defaultModel.javaClass.simpleName}")
        assertEquals("chat-completion-1", defaultModel.modelId)
    }

    @Test
    fun testTextCompletionModelDefaultFallsBackToPureCompletion() {
        // Test policy with only pure completion models
        val policyWithoutChat = object : PromptFxPolicy() {
            override fun modelInfo() = emptyList<ModelInfo>()
            override fun embeddingModels() = emptyList<EmbeddingService>()
            
            override fun textCompletionModels() = listOf(
                MockCompletion("pure-completion-1"),
                MockCompletion("pure-completion-2")
            )
            
            override fun chatModels() = emptyList<TextChat>()
            override fun multimodalModels() = emptyList<MultimodalChat>()
            override fun visionLanguageModels() = emptyList<VisionLanguageChat>()
            override fun imageModels() = emptyList<ImageGenerator>()
            override fun supportsView(simpleName: String) = true
            override val isShowBanner = false
            override val isShowUsage = false
            override val isShowApiKeyButton = false
            override val bar = PromptFxPolicyBar("Test", javafx.scene.paint.Color.GRAY, javafx.scene.paint.Color.WHITE)
        }

        val defaultModel = policyWithoutChat.textCompletionModelDefault()
        
        // Should fall back to first pure completion model when no chat models available
        assertEquals("pure-completion-1", defaultModel.modelId)
    }

    @Test
    fun testPureCompletionModelDefaultPrefersPureCompletions() {
        val defaultModel = testPolicy.pureCompletionModelDefault()
        
        // Should prefer pure completion models over chat-based completion models for CompletionsView
        assertFalse(defaultModel.javaClass.simpleName.contains("Chat"), 
            "Expected pure completion model default to be non-chat-based, but got: ${defaultModel.javaClass.simpleName}")
        assertEquals("pure-completion-1", defaultModel.modelId)
    }

    @Test
    fun testPureCompletionModelDefaultFallsBackToChatWhenNoPureModels() {
        // Test policy with only chat-based completion models
        val policyWithoutPureCompletion = object : PromptFxPolicy() {
            override fun modelInfo() = emptyList<ModelInfo>()
            override fun embeddingModels() = emptyList<EmbeddingService>()
            
            override fun textCompletionModels() = listOf(
                MockCompletionChat("chat-completion-1")
            )
            
            override fun chatModels() = emptyList<TextChat>()
            override fun multimodalModels() = emptyList<MultimodalChat>()
            override fun visionLanguageModels() = emptyList<VisionLanguageChat>()
            override fun imageModels() = emptyList<ImageGenerator>()
            override fun supportsView(simpleName: String) = true
            override val isShowBanner = false
            override val isShowUsage = false
            override val isShowApiKeyButton = false
            override val bar = PromptFxPolicyBar("Test", javafx.scene.paint.Color.GRAY, javafx.scene.paint.Color.WHITE)
        }

        val defaultModel = policyWithoutPureCompletion.pureCompletionModelDefault()
        
        // Should fall back to chat-based completion model when no pure completion models available
        assertEquals("chat-completion-1", defaultModel.modelId)
    }

    // Mock implementations for testing
    private class MockCompletion(override val modelId: String) : TextCompletion {
        override suspend fun complete(text: String, tokens: Int?, temperature: Double?, stop: String?, numResponses: Int?, history: List<TextChatMessage>) =
            AiPromptTrace.error<String>(null, "Mock implementation")
    }

    private class MockCompletionChat(override val modelId: String) : TextCompletion {
        override suspend fun complete(text: String, tokens: Int?, temperature: Double?, stop: String?, numResponses: Int?, history: List<TextChatMessage>) =
            AiPromptTrace.error<String>(null, "Mock implementation")
    }

    private class MockChat(override val modelId: String) : TextChat {
        override suspend fun chat(messages: List<TextChatMessage>, tokens: Int?, stop: List<String>?, requestJson: Boolean?, numResponses: Int?) =
            AiPromptTrace.error<TextChatMessage>(null, "Mock implementation")
    }

    private class MockEmbeddingService(override val modelId: String) : EmbeddingService {
        override fun chunkTextBySections(text: String, maxChunkSize: Int) = emptyList<TextChunk>()
        override suspend fun calculateEmbedding(text: List<String>, outputDimensionality: Int?): List<List<Double>> = emptyList()
    }
}