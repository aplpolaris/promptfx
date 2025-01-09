/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiPlanner

/**
 * View that gets result from a planned set of tasks from an [AiPlanner] object.
 * These tasks can be monitored while executing.
 */
abstract class AiPlanTaskView(title: String, description: String) : AiTaskView(title, description) {

    protected val common = ModelParameters()

    override suspend fun processUserInput(): AiPipelineResult<*> =
        AiPipelineExecutor.execute(plan().plan(), progress)

    abstract fun plan(): AiPlanner

}

