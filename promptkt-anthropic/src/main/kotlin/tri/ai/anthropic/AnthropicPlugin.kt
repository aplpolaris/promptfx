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

import tri.ai.core.*

/** Plugin registering Claude models and services via the Anthropic API. */
class AnthropicPlugin : TextPlugin {

    val client = AnthropicClient()

    override fun modelSource() = "Anthropic"

    override fun modelInfo() = if (client.isConfigured())
        AnthropicModelIndex.allModels().map { createModelInfo(it) }
    else
        emptyList()

    override fun embeddingModels() = models(AnthropicModelIndex.embeddingModels()) { TODO("Anthropic does not currently support embeddings") }

    override fun chatModels() =
        models(AnthropicModelIndex.chatModels()) { AnthropicTextChat(it, client) }

    override fun multimodalModels() =
        models(AnthropicModelIndex.multimodalModels()) { AnthropicMultimodalChat(it, client) }

    override fun textCompletionModels() =
        models(AnthropicModelIndex.completionModels()) { AnthropicTextCompletion(it, client) }

    override fun visionLanguageModels() =
        models(AnthropicModelIndex.visionLanguageModels()) { AnthropicVisionLanguageChat(it, client) }

    override fun imageGeneratorModels() = models(AnthropicModelIndex.imageGeneratorModels()) { TODO("Anthropic does not currently support image generation") }

    override fun close() {
        client.close()
    }

    private fun <T> models(ids: List<String>, factory: (String) -> T): List<T> =
        if (!client.isConfigured()) listOf() else ids.map(factory)

    private fun createModelInfo(modelId: String): ModelInfo {
        return ModelInfo(
            id = modelId,
            type = determineModelType(modelId),
            source = modelSource()
        ).also {
            it.name = modelId
            it.description = getModelDescription(modelId)
            it.inputTokenLimit = getInputTokenLimit(modelId)
            it.outputTokenLimit = getOutputTokenLimit(modelId)
            it.lifecycle = getModelLifecycle(modelId)
            it.inputs = getModelInputs(modelId)
            it.outputs = getModelOutputs(modelId)
        }
    }

    private fun determineModelType(modelId: String): ModelType {
        return when {
            modelId.startsWith("claude-3") && ("sonnet" in modelId || "opus" in modelId || "haiku" in modelId) -> ModelType.TEXT_VISION_CHAT
            modelId.startsWith("claude-") -> ModelType.TEXT_CHAT
            else -> ModelType.TEXT_CHAT
        }
    }

    private fun getModelDescription(modelId: String): String {
        return when {
            "claude-3-5-sonnet" in modelId -> "Claude 3.5 Sonnet - Most intelligent model for complex reasoning and analysis"
            "claude-3-5-haiku" in modelId -> "Claude 3.5 Haiku - Fast and cost-effective model for everyday tasks"
            "claude-3-opus" in modelId -> "Claude 3 Opus - Most powerful model for highly complex tasks"
            "claude-3-sonnet" in modelId -> "Claude 3 Sonnet - Balanced model for a wide range of tasks"
            "claude-3-haiku" in modelId -> "Claude 3 Haiku - Fastest model for simple queries and tasks"
            "claude-2.1" in modelId -> "Claude 2.1 - Previous generation model with extended context"
            "claude-2.0" in modelId -> "Claude 2.0 - Previous generation model"
            "claude-instant" in modelId -> "Claude Instant - Fast, affordable model for simpler tasks"
            else -> "Claude model by Anthropic"
        }
    }

    private fun getInputTokenLimit(modelId: String): Int {
        return when {
            "claude-3-5" in modelId -> 200_000
            "claude-3" in modelId -> 200_000
            "claude-2.1" in modelId -> 200_000
            "claude-2.0" in modelId -> 100_000
            "claude-instant" in modelId -> 100_000
            else -> 200_000
        }
    }

    private fun getOutputTokenLimit(modelId: String): Int {
        return when {
            "claude-3-5" in modelId -> 8_192
            "claude-3" in modelId -> 4_096
            "claude-2" in modelId -> 4_096
            "claude-instant" in modelId -> 4_096
            else -> 4_096
        }
    }

    private fun getModelLifecycle(modelId: String): ModelLifecycle {
        return when {
            "claude-3-5" in modelId -> ModelLifecycle.PRODUCTION
            "claude-3" in modelId -> ModelLifecycle.PRODUCTION
            "claude-2.1" in modelId -> ModelLifecycle.LEGACY
            "claude-2.0" in modelId -> ModelLifecycle.LEGACY
            "claude-instant" in modelId -> ModelLifecycle.LEGACY
            else -> ModelLifecycle.PRODUCTION
        }
    }

    private fun getModelInputs(modelId: String): List<DataModality>? {
        return when (determineModelType(modelId)) {
            ModelType.TEXT_CHAT -> listOf(DataModality.text)
            ModelType.TEXT_VISION_CHAT -> listOf(DataModality.text, DataModality.image)
            else -> listOf(DataModality.text)
        }
    }

    private fun getModelOutputs(modelId: String): List<DataModality>? {
        return when (determineModelType(modelId)) {
            ModelType.TEXT_CHAT -> listOf(DataModality.text)
            ModelType.TEXT_VISION_CHAT -> listOf(DataModality.text)
            else -> listOf(DataModality.text)
        }
    }

}