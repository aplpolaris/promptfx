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

/** Adapts a [MultimodalChat] for use where a [TextChat] is expected, by converting text messages to multimodal messages. */
class MultimodalChatTextAdapter(val inner: MultimodalChat) : TextChat {

    override val modelId = inner.modelId
    override val modelSource = inner.modelSource

    override fun toString() = inner.toString()

    override suspend fun chat(
        messages: List<TextChatMessage>,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?,
        requestJson: Boolean?
    ) = inner.chat(
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

/** Wraps this [MultimodalChat] as a [TextChat] adapter. */
fun MultimodalChat.asTextChat(): TextChat = MultimodalChatTextAdapter(this)
