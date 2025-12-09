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

import tri.ai.core.TextPlugin
import tri.ai.embedding.EmbeddingStrategy
import tri.ai.text.chunks.SmartTextChunker

/**
 * Global manager for models available within PromptFx.
 * Model availability is determined by the current [PromptFxPolicy].
 */
object PromptFxModels {

    var policy: PromptFxPolicy = PromptFxPolicyUnrestricted

    fun textCompletionModels() = policy.textCompletionModels()
    fun textCompletionModelDefault() = policy.textCompletionModelDefault()

    fun embeddingModels() = policy.embeddingModels()
    fun embeddingModelDefault() = policy.embeddingModelDefault()

    fun chatModels() = policy.chatModels()
    fun chatModelDefault() = policy.chatModelDefault()

    fun multimodalModels() = policy.multimodalModels()
    fun multimodalModelDefault() = policy.multimodalModelDefault()

    fun imageModels() = policy.imageModels()
    fun imageModelDefault() = policy.imageModelDefault()

    fun visionLanguageModels() = policy.visionLanguageModels()
    fun visionLanguageModelDefault() = policy.visionLanguageModelDefault()

    fun modelIds() = (
            textCompletionModels().map { it.modelId } +
            embeddingModels().map { it.modelId } +
            chatModels().map { it.modelId } +
            multimodalModels().map { it.modelId } +
            imageModels().map { it.modelId } +
            visionLanguageModels().map { it.modelId }
        ).toSet()

    //region SOURCED MODEL WRAPPERS

    /**
     * Returns text completion models wrapped with source information for UI display.
     * Models from the same plugin will have the same source.
     */
    fun sourcedTextCompletionModels(): List<SourcedTextCompletion> =
        policy.supportedPlugins().flatMap { plugin ->
            plugin.textCompletionModels().map { model ->
                SourcedTextCompletion(model, plugin.modelSource())
            }
        }

    /**
     * Returns chat models wrapped with source information for UI display.
     * Models from the same plugin will have the same source.
     */
    fun sourcedChatModels(): List<SourcedTextChat> =
        policy.supportedPlugins().flatMap { plugin ->
            plugin.chatModels().map { model ->
                SourcedTextChat(model, plugin.modelSource())
            }
        }

    /**
     * Returns embedding models wrapped with source information for UI display.
     * Models from the same plugin will have the same source.
     */
    fun sourcedEmbeddingModels(): List<SourcedEmbeddingModel> =
        policy.supportedPlugins().flatMap { plugin ->
            plugin.embeddingModels().map { model ->
                SourcedEmbeddingModel(model, plugin.modelSource())
            }
        }

    /**
     * Returns embedding strategies wrapped with source information for UI display.
     */
    fun sourcedEmbeddingStrategies(): List<SourcedEmbeddingStrategy> =
        policy.supportedPlugins().flatMap { plugin ->
            plugin.embeddingModels().map { model ->
                SourcedEmbeddingStrategy(EmbeddingStrategy(model, SmartTextChunker()), plugin.modelSource())
            }
        }

    /**
     * Returns multimodal chat models wrapped with source information for UI display.
     * Models from the same plugin will have the same source.
     */
    fun sourcedMultimodalModels(): List<SourcedMultimodalChat> =
        policy.supportedPlugins().flatMap { plugin ->
            plugin.multimodalModels().map { model ->
                SourcedMultimodalChat(model, plugin.modelSource())
            }
        }

    /**
     * Returns vision language chat models wrapped with source information for UI display.
     * Models from the same plugin will have the same source.
     */
    fun sourcedVisionLanguageModels(): List<SourcedVisionLanguageChat> =
        policy.supportedPlugins().flatMap { plugin ->
            plugin.visionLanguageModels().map { model ->
                SourcedVisionLanguageChat(model, plugin.modelSource())
            }
        }

    /**
     * Returns image generator models wrapped with source information for UI display.
     * Models from the same plugin will have the same source.
     */
    fun sourcedImageModels(): List<SourcedImageGenerator> =
        policy.supportedPlugins().flatMap { plugin ->
            plugin.imageGeneratorModels().map { model ->
                SourcedImageGenerator(model, plugin.modelSource())
            }
        }

    //endregion

}
