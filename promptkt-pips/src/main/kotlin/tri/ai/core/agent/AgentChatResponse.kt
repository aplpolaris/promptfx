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
package tri.ai.core.agent

import tri.ai.core.MultimodalChatMessage

/** Response from sending a message to an agent chat. */
data class AgentChatResponse(
    /** The agent's response message. */
    val message: MultimodalChatMessage,
    /** Any reasoning/thought process (if enabled). */
    val reasoning: String? = null,
    /** Metadata about the response. */
    val metadata: Map<String, Any> = emptyMap()
)