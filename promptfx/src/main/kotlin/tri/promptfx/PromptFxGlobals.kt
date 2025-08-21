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
package tri.promptfx

import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.fill

/** Unified access to global objects within [PromptFx]. */
object PromptFxGlobals {

    /** Prompt library. */
    val promptLibrary = PromptLibrary.INSTANCE

    /** Gets prompt ids with a given prefix. */
    fun promptsWithPrefix(prefix: String) =
        promptLibrary.list(prefix = prefix).map { it.id }

    /** Lookup a prompt with given id. */
    fun lookupPrompt(promptId: String) =
        promptLibrary.get(promptId) ?: error("Prompt '$promptId' not found in library")

    /** Fills a prompt with the given values. */
    fun fillPrompt(promptId: String, vararg values: Pair<String, Any>) =
        promptLibrary.get(promptId)?.fill(*values) ?: error("Prompt '$promptId' not found in library")

}
