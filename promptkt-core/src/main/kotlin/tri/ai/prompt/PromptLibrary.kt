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
package tri.ai.prompt

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Manages collections of prompt templates.
 * TODO - versioning is not implemented, even though there's a placeholder for it in prompt id conventions
 */
class PromptLibrary {

    // indices (id is full id = group/name@x.y.z, bare = group/name only)
    // category and tag indices are lists of ids
    private val byId = mutableMapOf<String, PromptDef>()
    private val byBare = mutableMapOf<String, MutableList<PromptDef>>()
    private val byCategory = mutableMapOf<String, MutableList<String>>()
    private val byTag = mutableMapOf<String, MutableList<String>>()

    //region INDEXING

    /** Adds a group to the library, indexing its prompts. */
    fun addGroup(group: PromptGroup) {
        group.resolved().prompts.forEach {
            addPrompt(it)
        }
    }

    /** Adds a prompt to the library, indexing it by id, bare id, category, and tags. */
    fun addPrompt(prompt: PromptDef) {
        prompt.resolved(PromptGroup("Uncategorized")).let {
            byId[it.id] = it
            byBare.getOrPut(it.bareId) { mutableListOf() }.add(it)
            it.category?.let { category ->
                byCategory.getOrPut(category) { mutableListOf() }.add(it.id)
            }
            it.tags.forEach { tag ->
                byTag.getOrPut(tag) { mutableListOf() }.add(it.id)
            }
        }
    }

    private fun reindex(prompts: List<PromptDef>) {
        byId.clear()
        byBare.clear()
        byCategory.clear()
        byTag.clear()
        prompts.forEach { addPrompt(it) }
    }

    //endregion

    /** Get a prompt by exact id or by bare id (returns latest). */
    fun get(idOrBare: String): PromptDef? = resolve(idOrBare)?.let { byId[it] }

    /** Resolve idOrBare to a concrete versioned id. */
    fun resolve(idOrBare: String): String? {
        return if (idOrBare.contains('@')) {
            if (byId.containsKey(idOrBare)) idOrBare else null
        } else {
            byBare[idOrBare]?.firstOrNull()?.id
        }
    }

    /** List all prompts (optionally filter). */
    fun list(category: String? = null, tag: String? = null, prefix: String? = null): List<PromptDef> {
        val foundCategory = if (category == null) byId.values else byCategory[category]?.mapNotNull { byId[it] } ?: listOf<PromptDef>()
        val foundTag = if (tag == null) byId.values else byTag[tag]?.mapNotNull { byId[it] } ?: listOf<PromptDef>()
        return foundCategory.intersect(foundTag)
            .filter { prefix == null || it.id.startsWith(prefix) }
            .sortedBy { it.id }
    }

    /** List all prompts with a custom filter. */
    fun list(filter: (PromptDef) -> Boolean) =
        byId.values.filter(filter).sortedBy { it.id }

    companion object {

        val RUNTIME_INSTANCE: PromptLibrary = loadRuntimePromptLibrary()
        val INSTANCE: PromptLibrary = loadDefaultPromptLibrary()

        fun refreshRuntimePrompts() {
            INSTANCE.reindex(loadDefaultPromptLibrary().list())
            RUNTIME_INSTANCE.reindex(loadRuntimePromptLibrary().list())
        }

        private fun loadDefaultPromptLibrary() = PromptLibrary().apply {
            readFromResourceDirectory()
            readFromRuntimeDirectory()
        }

        private fun loadRuntimePromptLibrary() = PromptLibrary().apply {
            readFromRuntimeDirectory()
        }

        // load from resource directory
        private fun PromptLibrary.readFromResourceDirectory() {
            PromptGroupIO.readAllFromResourceDirectory().forEach {
                addGroup(it)
            }
        }

        // load from prompts/ directory, recursively, if it exists
        private fun PromptLibrary.readFromRuntimeDirectory() {
            Path("prompts/").let {
                if (it.exists() && it.isDirectory())
                    PromptGroupIO.readFromDirectory(it, recursive = true).forEach {
                        addGroup(it.resolved())
                    }
            }
        }

        inline fun <reified T> readFromResourceDirectory() =
            PromptLibrary().apply {
                PromptGroupIO.readAllFromResourceDirectory(T::class.java.`package`.name+".resources").forEach {
                    addGroup(it)
                }
            }

        val RUNTIME_PROMPTS_FILE = File("prompts/custom-prompts.yaml")

        fun createRuntimePromptsFile() {
            val file = RUNTIME_PROMPTS_FILE
            file.parentFile?.mkdirs()
            file.createNewFile()
            file.writeText(PromptLibrary::class.java.getResource("resources/custom-prompts.yaml.template")!!.readText())
        }

        /** Load a custom prompt library from a specific file or directory path. */
        fun loadFromPath(path: String): PromptLibrary = PromptLibrary().apply {
            val targetPath = Path(path)
            when {
                targetPath.exists() && targetPath.isDirectory() -> {
                    PromptGroupIO.readFromDirectory(targetPath, recursive = true).forEach {
                        addGroup(it.resolved())
                    }
                }
                targetPath.exists() -> {
                    addGroup(PromptGroupIO.readFromFile(targetPath).resolved())
                }
                else -> throw IllegalArgumentException("Prompt library path does not exist: $path")
            }
        }

        //endregion

    }

}