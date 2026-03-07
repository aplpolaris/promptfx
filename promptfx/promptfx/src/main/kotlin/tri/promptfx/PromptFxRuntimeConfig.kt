/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx

import tri.ai.prompt.PromptDef
import tri.util.info
import java.io.File
import java.util.Properties

/**
 * Runtime configuration loaded from `PromptFx.properties` in the working directory or `config/` subdirectory.
 * Controls prompt and model visibility via include/exclude glob patterns.
 *
 * Supported properties:
 * - `prompt.include` – comma-separated glob patterns matched against prompt id and/or category; if set, only matching prompts are active
 * - `prompt.exclude` – comma-separated glob patterns matched against prompt id and/or category; matching prompts are deactivated
 * - `model.include` – comma-separated glob patterns matched against model id; if set, only matching models are active
 * - `model.exclude` – comma-separated glob patterns matched against model id; matching models are deactivated
 */
object PromptFxRuntimeConfig {

    private const val FILENAME = "PromptFx.properties"
    private const val PROP_PROMPT_INCLUDE = "prompt.include"
    private const val PROP_PROMPT_EXCLUDE = "prompt.exclude"
    private const val PROP_MODEL_INCLUDE = "model.include"
    private const val PROP_MODEL_EXCLUDE = "model.exclude"

    /** Glob patterns that a prompt's id or category must match (at least one) to be active. Empty = no restriction. */
    var promptIncludePatterns: List<String> = listOf()
        private set
    /** Glob patterns that, if matched by a prompt's id or category, deactivate the prompt. */
    var promptExcludePatterns: List<String> = listOf()
        private set
    /** Glob patterns that a model id must match (at least one) to be active. Empty = no restriction. */
    var modelIncludePatterns: List<String> = listOf()
        private set
    /** Glob patterns that, if matched by a model id, deactivate the model. */
    var modelExcludePatterns: List<String> = listOf()
        private set

    init {
        reload()
    }

    /** Reload configuration from disk. */
    fun reload() {
        val props = loadProperties()
        promptIncludePatterns = props.parseGlobs(PROP_PROMPT_INCLUDE)
        promptExcludePatterns = props.parseGlobs(PROP_PROMPT_EXCLUDE)
        modelIncludePatterns = props.parseGlobs(PROP_MODEL_INCLUDE)
        modelExcludePatterns = props.parseGlobs(PROP_MODEL_EXCLUDE)
        if (hasActiveFilters()) {
            info<PromptFxRuntimeConfig>("Loaded runtime config: promptInclude=$promptIncludePatterns, promptExclude=$promptExcludePatterns, modelInclude=$modelIncludePatterns, modelExclude=$modelExcludePatterns")
        }
    }

    //region FILTERS

    /**
     * Returns `true` if the given prompt is *active* (not filtered out).
     * A prompt is active if it matches at least one include pattern (or no include patterns are defined)
     * and does not match any exclude pattern.
     * Patterns are matched against both the prompt's id and its category.
     */
    fun isPromptActive(prompt: PromptDef): Boolean {
        val targets = listOfNotNull(prompt.id, prompt.category)
        val included = promptIncludePatterns.isEmpty() || targets.any { matchesAny(it, promptIncludePatterns) }
        val excluded = targets.any { matchesAny(it, promptExcludePatterns) }
        return included && !excluded
    }

    /**
     * Returns `true` if the given model id is *active* (not filtered out).
     * A model is active if it matches at least one include pattern (or no include patterns are defined)
     * and does not match any exclude pattern.
     */
    fun isModelActive(modelId: String): Boolean {
        val included = modelIncludePatterns.isEmpty() || matchesAny(modelId, modelIncludePatterns)
        val excluded = matchesAny(modelId, modelExcludePatterns)
        return included && !excluded
    }

    //endregion

    //region INTERNAL HELPERS

    private fun loadProperties(): Properties {
        val candidates = listOf(File(FILENAME), File("config/$FILENAME"))
        val file = candidates.firstOrNull { it.exists() } ?: return Properties()
        info<PromptFxRuntimeConfig>("Loading runtime configuration from ${file.absolutePath}")
        return Properties().apply { file.inputStream().use { load(it) } }
    }

    private fun Properties.parseGlobs(key: String): List<String> =
        getProperty(key)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: listOf()

    private fun matchesAny(value: String, patterns: List<String>): Boolean =
        patterns.any { matchesGlob(value, it) }

    internal fun matchesGlob(value: String, pattern: String): Boolean {        // Convert glob pattern to regex: ** -> .*, * -> [^/]*, ? -> [^/], others are regex-escaped
        val regex = buildString {
            var i = 0
            while (i < pattern.length) {
                when {
                    pattern[i] == '*' && i + 1 < pattern.length && pattern[i + 1] == '*' -> {
                        append(".*")
                        i += 2
                    }
                    pattern[i] == '*' -> { append("[^/]*"); i++ }
                    pattern[i] == '?' -> { append("[^/]"); i++ }
                    else -> { append(Regex.escape(pattern[i].toString())); i++ }
                }
            }
        }
        return Regex(regex).matches(value)
    }

    private fun hasActiveFilters() =
        promptIncludePatterns.isNotEmpty() || promptExcludePatterns.isNotEmpty() ||
        modelIncludePatterns.isNotEmpty() || modelExcludePatterns.isNotEmpty()

    //endregion
}
