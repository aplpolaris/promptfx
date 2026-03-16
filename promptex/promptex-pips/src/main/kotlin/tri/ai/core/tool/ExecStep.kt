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
package tri.ai.core.tool

/**
 * Sealed class representing a single step in an executor's run, persisted in [ExecContext.steps].
 *
 * Provides a chronological, structured audit trail that complements the [tri.ai.pips.ExecEvent] stream.
 * While [tri.ai.pips.ExecEvent] is emitted in real-time for live observers, [ExecStep] is recorded in
 * [ExecContext] so that the full execution history is available for inspection after the run completes —
 * regardless of which executor type produced it.
 */
sealed class ExecStep {
    abstract val timestampMs: Long

    /** Records a tool or solver invocation and its result. */
    data class ToolCall(
        val toolName: String,
        val input: String,
        val output: String,
        val durationMs: Long,
        override val timestampMs: Long = System.currentTimeMillis(),
    ) : ExecStep()

    /** Records a reasoning or thought step (e.g. from a ReAct chain). */
    data class Reasoning(
        val content: String,
        override val timestampMs: Long = System.currentTimeMillis(),
    ) : ExecStep()

    /** Records a planning or task-decomposition update. */
    data class PlanUpdate(
        val description: String,
        override val timestampMs: Long = System.currentTimeMillis(),
    ) : ExecStep()

    /** Records the completion or failure of a named task. */
    data class TaskResult(
        val taskId: String,
        val succeeded: Boolean,
        val durationMs: Long,
        override val timestampMs: Long = System.currentTimeMillis(),
    ) : ExecStep()
}
