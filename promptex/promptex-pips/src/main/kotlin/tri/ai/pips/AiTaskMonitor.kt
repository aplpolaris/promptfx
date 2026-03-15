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
package tri.ai.pips

import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.tool.ExecContext
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/**
 * Monitors execution status of tasks via [ExecEvent] emissions.
 * This replaces the original callback-based interface with a [FlowCollector]<[ExecEvent]>-based model,
 * so that task lifecycle and agent chat events share a single unified event stream.
 */
typealias AiTaskMonitor = FlowCollector<ExecEvent>

/** Monitor that silently ignores all events. */
object IgnoreMonitor : FlowCollector<ExecEvent> {
    override suspend fun emit(value: ExecEvent) {}
}

/**
 * Reports sub-progress within a task with a descriptive message and a fractional progress value (0.0–1.0).
 * Emits a [ExecEvent.TaskUpdate] carrying a temporary [AiTask] whose id holds the message.
 */
suspend fun FlowCollector<ExecEvent>.progressUpdate(message: String, progress: Double) =
    emitTaskUpdate(object : AiTask<Any?, Any?>(message) {
        override suspend fun execute(input: Any?, context: ExecContext) =
            AiPromptTrace(outputInfo = AiOutputInfo.text(message))
    }, progress)
