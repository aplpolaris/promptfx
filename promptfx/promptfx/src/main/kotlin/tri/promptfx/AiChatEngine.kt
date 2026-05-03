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

import tri.ai.core.AiChatEngine
import tri.ai.core.CompletionBuilder
import tri.ai.core.execute
import tri.ai.pips.AiTaskBuilder

/** Creates a single-task [AiTaskBuilder] from a [CompletionBuilder] dispatching to the appropriate chat interface. */
fun CompletionBuilder.taskPlan(engine: AiChatEngine): AiTaskBuilder<*> {
    val taskId = id ?: when (engine) {
        is AiChatEngine.Text -> "text-chat"
        is AiChatEngine.Multimodal -> "multimodal-chat"
    }
    return AiTaskBuilder.task(taskId) { context ->
        val result = execute(engine)
        context.logTrace(taskId, result)
        result.output?.outputs?.firstOrNull()?.textContent(ifNone = "") ?: ""
    }
}
