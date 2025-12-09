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

import tri.ai.core.*
import tri.ai.embedding.EmbeddingStrategy

/**
 * Wrapper for a text completion model that includes source information.
 * Used in the UI to disambiguate models with the same ID from different sources.
 */
data class SourcedTextCompletion(
    val model: TextCompletion,
    val source: String
) {
    val modelId: String get() = model.modelId
    override fun toString() = "$modelId [$source]"
}

/**
 * Wrapper for a text chat model that includes source information.
 * Used in the UI to disambiguate models with the same ID from different sources.
 */
data class SourcedTextChat(
    val model: TextChat,
    val source: String
) {
    val modelId: String get() = model.modelId
    override fun toString() = "$modelId [$source]"
}

/**
 * Wrapper for an embedding model that includes source information.
 * Used in the UI to disambiguate models with the same ID from different sources.
 */
data class SourcedEmbeddingModel(
    val model: EmbeddingModel,
    val source: String
) {
    val modelId: String get() = model.modelId
    override fun toString() = "$modelId [$source]"
}

/**
 * Wrapper for a multimodal chat model that includes source information.
 * Used in the UI to disambiguate models with the same ID from different sources.
 */
data class SourcedMultimodalChat(
    val model: MultimodalChat,
    val source: String
) {
    val modelId: String get() = model.modelId
    override fun toString() = "$modelId [$source]"
}

/**
 * Wrapper for a vision language chat model that includes source information.
 * Used in the UI to disambiguate models with the same ID from different sources.
 */
data class SourcedVisionLanguageChat(
    val model: VisionLanguageChat,
    val source: String
) {
    val modelId: String get() = model.modelId
    override fun toString() = "$modelId [$source]"
}

/**
 * Wrapper for an image generator model that includes source information.
 * Used in the UI to disambiguate models with the same ID from different sources.
 */
data class SourcedImageGenerator(
    val model: ImageGenerator,
    val source: String
) {
    val modelId: String get() = model.modelId
    override fun toString() = "$modelId [$source]"
}

/**
 * Wrapper for an embedding strategy that includes source information.
 * Used in the UI to disambiguate embedding models with the same ID from different sources.
 */
data class SourcedEmbeddingStrategy(
    val strategy: EmbeddingStrategy,
    val source: String
) {
    val modelId: String get() = strategy.model.modelId
    override fun toString() = "$modelId [$source]"
}
