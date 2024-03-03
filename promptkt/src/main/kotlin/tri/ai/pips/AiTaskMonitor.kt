/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

/** Tracks status of tasks. */
interface AiTaskMonitor {
    fun taskStarted(task: AiTask<*>)
    fun taskUpdate(task: AiTask<*>, progress: Double)
    fun taskCompleted(task: AiTask<*>, result: Any?)
    fun taskFailed(task: AiTask<*>, error: Throwable)
}

class IgnoreMonitor : AiTaskMonitor {
    override fun taskStarted(task: AiTask<*>) {}
    override fun taskUpdate(task: AiTask<*>, progress: Double) {}
    override fun taskCompleted(task: AiTask<*>, result: Any?) {}
    override fun taskFailed(task: AiTask<*>, error: Throwable) {}
}
