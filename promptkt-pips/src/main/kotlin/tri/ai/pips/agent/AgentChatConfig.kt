/*-
 * #%L
 * tri.promptfx:promptkt-pips
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
package tri.ai.pips.agent

/** Configuration for an agent chat session. */
data class AgentChatConfig(
    /** Model ID to use for the chat. */
    val modelId: String = "gpt-4o-mini",
    /** Maximum number of messages to keep in context. */
    val maxContextMessages: Int = 20,
    /** System message for the agent. */
    val systemMessage: String? = null,
    /** Temperature for response generation. */
    val temperature: Double = 0.7,
    /** Maximum tokens for response. */
    val maxTokens: Int = 4000,
    /** Whether to enable tool use. */
    val enableTools: Boolean = true,
    /** Whether to enable reasoning/thoughts display. */
    val enableReasoningMode: Boolean = false
)