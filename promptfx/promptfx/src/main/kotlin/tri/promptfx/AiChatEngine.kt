/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import tri.ai.core.*
import tri.ai.pips.AiTaskBuilder

/**
 * A unified chat engine for the PromptFx UI that can dispatch to either a [TextChat] or [MultimodalChat] model.
 * This type is used in the UI controller ([PromptFxController.chatEngine]) to allow selecting any supported model.
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
suspend fun CompletionBuilder.execute(engine: AiChatEngine): tri.ai.prompt.trace.AiPromptTrace = when (engine) {
    is AiChatEngine.Text -> execute(engine.model)
    is AiChatEngine.Multimodal -> execute(engine.model)
}

/** Creates a single-task [AiTaskBuilder] from a [CompletionBuilder] dispatching to the appropriate chat interface. */
fun CompletionBuilder.taskPlan(engine: AiChatEngine) = when (engine) {
    is AiChatEngine.Text -> AiTaskBuilder.task(id ?: "text-chat") { execute(engine.model) }
    is AiChatEngine.Multimodal -> AiTaskBuilder.task(id ?: "multimodal-chat") { execute(engine.model) }
}
