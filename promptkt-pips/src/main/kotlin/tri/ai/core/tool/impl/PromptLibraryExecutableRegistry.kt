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
package tri.ai.core.tool.impl

import tri.ai.core.tool.ExecutableRegistry
import tri.ai.prompt.PromptLibrary

/** Creates an executable registry from a prompt library file. */
class PromptLibraryExecutableRegistry(private val lib: PromptLibrary): ExecutableRegistry {

    private val executables by lazy {
        lib.list().associate { def ->
            val exec = PromptExecutable(def)
            exec.name to exec
        }
    }

    override fun get(name: String) = executables[name]

    override fun list() = executables.values.toList()

}
