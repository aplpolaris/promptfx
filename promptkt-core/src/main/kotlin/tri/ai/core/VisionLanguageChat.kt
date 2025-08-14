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
package tri.ai.core

import tri.ai.prompt.trace.AiPromptTrace
import java.net.URI

/**
 * An interface for completing vision-language chats.
 */
interface VisionLanguageChat {

    val modelId: String

    /** Completes user text. */
    suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double? = null,
        tokens: Int? = 1000,
        stop: List<String>? = null,
        requestJson: Boolean? = null
    ): AiPromptTrace<TextChatMessage>

}

/** A single message in a vision-language chat. */
data class VisionLanguageChatMessage(val role: MChatRole, val content: String, val image: URI)
