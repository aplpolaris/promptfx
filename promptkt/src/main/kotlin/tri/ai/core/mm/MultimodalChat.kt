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
package tri.ai.core.mm

import tri.ai.prompt.trace.AiPromptTrace

/**
 * Interface for a universal chat completion capability with a single model, designed to support multistep conversations
 * and multipart requests with combined modalities. Results may also have multiple modalities. Also supports techniques
 * for constrained outputs such as grammars, etc. Provides capacity for exceptions when models do not support a given
 * capability.
 */
interface MultimodalChat {

    /** Identifier for underlying model. */

    val modelId: String

    /** Provided a response to a sequence of chat messages. */
    suspend fun chat(
        messages: List<MultimodalChatMessage>,
        parameters: MChatParameters = MChatParameters()
    ): AiPromptTrace<MultimodalChatMessage>

    /** Provide a response to a single chat message. */
    suspend fun chat(
        message: MultimodalChatMessage,
        parameters: MChatParameters = MChatParameters()
    ): AiPromptTrace<MultimodalChatMessage> =
        chat(listOf(message), parameters)
}
