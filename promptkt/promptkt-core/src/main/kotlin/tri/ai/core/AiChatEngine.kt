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
package tri.ai.core

/**
 * A unified chat engine that can dispatch to either a [TextChat] or [MultimodalChat] model.
 * Use [asTextChat] to get a [TextChat] view regardless of the underlying type.
 */
sealed class AiChatEngine : AiModel {

    /** Wraps a [TextChat] model. */
    class Text(val model: TextChat) : AiChatEngine() {
        override val modelId = model.modelId
        override val modelSource = model.modelSource
        override fun toString() = model.toString()
    }

    /** Wraps a [MultimodalChat] model. */
    class Multimodal(val model: MultimodalChat) : AiChatEngine() {
        override val modelId = model.modelId
        override val modelSource = model.modelSource
        override fun toString() = model.toString()
    }

    /**
     * Returns a [TextChat] view of this engine, adapting if needed.
     * For [Multimodal] engines, text messages are converted to [MultimodalChatMessage] before dispatch.
     */
    fun asTextChat(): TextChat = when (this) {
        is Text -> model
        is Multimodal -> object : TextChat {
            override val modelId = this@AiChatEngine.modelId
            override val modelSource = this@AiChatEngine.modelSource
            override fun toString() = this@AiChatEngine.toString()
            override suspend fun chat(
                messages: List<TextChatMessage>,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?,
                requestJson: Boolean?
            ) = model.chat(
                messages.map { MultimodalChatMessage.text(it.role, it.content ?: "") },
                MChatParameters(
                    variation = variation,
                    tokens = tokens,
                    stop = stop,
                    responseFormat = if (requestJson == true) MResponseFormat.JSON else MResponseFormat.TEXT,
                    numResponses = numResponses
                )
            )
        }
    }

    companion object {
        fun from(model: TextChat): AiChatEngine = Text(model)
        fun from(model: MultimodalChat): AiChatEngine = Multimodal(model)
    }
}

/** Executes a [CompletionBuilder] task dispatching to the appropriate chat interface. */
suspend fun CompletionBuilder.execute(engine: AiChatEngine): tri.ai.prompt.trace.AiTaskTrace = when (engine) {
    is AiChatEngine.Text -> execute(engine.model)
    is AiChatEngine.Multimodal -> execute(engine.model)
}

/**
 * Resolves a model ID to an [AiChatEngine], checking [TextChat] models first, then [MultimodalChat] models.
 * An optional [providerHint] narrows the search to models from a specific source (case-insensitive).
 * If [providerHint] matches no model the search falls back to any provider.
 * Throws [NoSuchElementException] if no model with the given ID is found.
 */
fun AiModelProvider.Companion.chatEngine(modelId: String, providerHint: String? = null): AiChatEngine {
    val (parsedId, parsedSource) = AiModelProvider.parseModelId(modelId)
    val source = parsedSource.ifEmpty { providerHint }
    fun exact(m: AiModel) = m.modelId == parsedId && (source.isNullOrEmpty() || m.modelSource.equals(source, ignoreCase = true))
    fun loose(m: AiModel) = m.modelId == parsedId
    // Try provider-scoped match first, then fall back to any provider
    chatModels().firstOrNull { exact(it) }?.let { return AiChatEngine.from(it) }
    multimodalModels().firstOrNull { exact(it) }?.let { return AiChatEngine.from(it) }
    if (!source.isNullOrEmpty()) {
        chatModels().firstOrNull { loose(it) }?.let { return AiChatEngine.from(it) }
        multimodalModels().firstOrNull { loose(it) }?.let { return AiChatEngine.from(it) }
    }
    throw NoSuchElementException("No chat or multimodal model found with id '$modelId'")
}

/**
 * Returns all models usable as chat engines — both [TextChat] and [MultimodalChat] models,
 * with [TextChat] listed first and duplicates (same modelId + source) deduplicated.
 */
fun AiModelProvider.Companion.allChatEngines(): List<AiChatEngine> {
    val seen = mutableSetOf<Pair<String, String>>()
    val result = mutableListOf<AiChatEngine>()
    for (m in chatModels()) {
        if (seen.add(m.modelId to m.modelSource)) result.add(AiChatEngine.from(m))
    }
    for (m in multimodalModels()) {
        if (seen.add(m.modelId to m.modelSource)) result.add(AiChatEngine.from(m))
    }
    return result
}
