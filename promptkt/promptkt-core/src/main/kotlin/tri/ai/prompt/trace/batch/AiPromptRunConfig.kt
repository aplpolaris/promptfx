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
package tri.ai.prompt.trace.batch

import tri.ai.core.TextChat
import tri.ai.core.AiModelProvider
import tri.ai.prompt.trace.*

/** Configuration required for executing a text completion prompt. */
class AiPromptRunConfig(
    val promptInfo: PromptInfo,
    val modelInfo: AiModelInfo,
    val modelLookup: (String) -> TextChat = { AiModelProvider.chatModel(it) }
) {
    override fun toString() =
        "AiPromptRunConfig(promptInfo=$promptInfo, modelInfo=$modelInfo)"
}
