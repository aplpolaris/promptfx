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
package tri.ai.pips.api

import tri.ai.core.tool.ExecutableRegistry

/** Validates a [PPlan] object. */
object PPlanValidator {

    /** Checks there are no duplicate saveAs names. */
    fun validateNames(plan: PPlan) {
        val dups = plan.steps.mapNotNull { it.saveAs }.groupBy { it }.filter { it.value.size > 1 }
        require(dups.isEmpty()) { "Duplicate saveAs vars: ${dups.keys}" }
    }

    /** Checks that all tools exist within the registry */
    fun validateToolsExist(plan: PPlan, registry: ExecutableRegistry) {
        val missing = plan.steps.map { it.tool }.filter { registry.get(it) == null }
        require(missing.isEmpty()) {
            "Unknown tools: $missing" +
            "    Valid tools: ${registry.list().map { it.name }}"
        }
    }

    /** Checks that the plan has at least one step. */
    fun validateHasSteps(plan: PPlan) {
        require(plan.steps.isNotEmpty()) { "Plan has no steps." }
    }

}
