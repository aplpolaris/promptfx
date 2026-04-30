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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID.randomUUID

/**
 * Identity descriptor for an [AiTaskTrace], grouping the three task-identity fields together.
 *
 * - **[taskId]** — unique identifier for this task execution (default: random UUID).
 * - **[parentTaskId]** — identifier of the parent task, if this is a sub-task. Used to reconstruct
 *   call graphs and parent/child hierarchies.
 * - **[callerId]** — identifier of the caller or view that initiated this task (e.g. the UI view name,
 *   agent name, or pipeline step label). Replaces the former `viewId` field.
 *
 * Compatible with OTel/LangFuse span context conventions.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiTaskId(
    /** Unique identifier for this task execution. */
    var taskId: String = randomUUID().toString(),
    /** Identifier of the parent task, enabling reconstruction of call graphs. */
    var parentTaskId: String? = null,
    /** Identifier of the caller or system component that initiated this task. */
    var callerId: String? = null
)
