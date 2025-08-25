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
package tri.ai.pips.core

/** Registry so plan names resolve to code. */
interface ExecutableRegistry {
    fun get(name: String): Executable?
    fun list(): List<Executable>

    companion object {

        /** Creates a registry from a list of executables. */
        fun create(listOf: List<Executable>) = object : ExecutableRegistry {
            val index = listOf.associateBy { it.name }
            override fun get(name: String) = index[name]
            override fun list() = index.values.toList()
        }

    }
}

/**
 * Registry merging multiple other registries.
 * Naive implementation allows executables in later registries to override earlier ones.
 */
class MergedExecutableRegistry(registries: List<ExecutableRegistry>) : ExecutableRegistry {
    private val mergedIndex = registries
        .flatMap { it.list() }
        .associateBy { it.name }
    override fun get(name: String) = mergedIndex[name]
    override fun list() = mergedIndex.values.toList()
}
