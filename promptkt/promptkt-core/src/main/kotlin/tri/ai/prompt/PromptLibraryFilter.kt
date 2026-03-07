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
package tri.ai.prompt

import com.fasterxml.jackson.annotation.JsonInclude
import tri.util.info
import tri.util.json.yamlMapper
import java.io.File

/**
 * Filter configuration for [PromptLibrary] controlling which prompts are loaded.
 *
 * Includes are applied first: if any include patterns are specified (via [includeIds] or
 * [includeCategories]), only prompts matching at least one include pattern are kept.
 * If no include patterns are specified, all prompts pass the include check.
 * Excludes ([excludeIds], [excludeCategories]) are then applied to remove matching prompts.
 *
 * All patterns use glob-style matching: `*` matches any sequence of characters,
 * `?` matches a single character. Matching is case-insensitive.
 * ID patterns are matched against a prompt's bare ID (without version suffix).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class PromptLibraryFilter(
    /** Glob patterns for prompt bare IDs to include (e.g. `"text-summarize/summarize"`). */
    val includeIds: List<String> = emptyList(),
    /** Glob patterns for prompt categories to include (e.g. `"text"`). */
    val includeCategories: List<String> = emptyList(),
    /** Glob patterns for prompt bare IDs to exclude. Applied after includes. */
    val excludeIds: List<String> = emptyList(),
    /** Glob patterns for prompt categories to exclude. Applied after includes. */
    val excludeCategories: List<String> = emptyList()
) {

    /** Whether this filter accepts all prompts with no restrictions. */
    val isAcceptAll: Boolean
        get() = includeIds.isEmpty() && includeCategories.isEmpty() &&
                excludeIds.isEmpty() && excludeCategories.isEmpty()

    /** Returns true if the given prompt passes this filter. */
    fun accepts(prompt: PromptDef): Boolean {
        val hasIncludes = includeIds.isNotEmpty() || includeCategories.isNotEmpty()
        if (hasIncludes) {
            val idMatch = includeIdPatterns.any { it.matches(prompt.bareId) }
            val catMatch = prompt.category != null && includeCategoryPatterns.any { it.matches(prompt.category) }
            if (!idMatch && !catMatch) return false
        }
        if (excludeIdPatterns.any { it.matches(prompt.bareId) }) return false
        if (prompt.category != null && excludeCategoryPatterns.any { it.matches(prompt.category) }) return false
        return true
    }

    private val includeIdPatterns: List<Regex> by lazy { includeIds.map(::globToRegex) }
    private val includeCategoryPatterns: List<Regex> by lazy { includeCategories.map(::globToRegex) }
    private val excludeIdPatterns: List<Regex> by lazy { excludeIds.map(::globToRegex) }
    private val excludeCategoryPatterns: List<Regex> by lazy { excludeCategories.map(::globToRegex) }

    companion object {
        private const val CONFIG_FILE_NAME = "prompt-library.yaml"

        /** Load filter from a runtime config file or the built-in default resource. */
        fun load(): PromptLibraryFilter = loadFromFileOrDefault(null)

        /**
         * Load filter from the given [path] if it exists, otherwise fall back to a runtime
         * config file ([CONFIG_FILE_NAME] or `config/[CONFIG_FILE_NAME]`) or the built-in
         * default resource.
         */
        fun loadFromFileOrDefault(path: String?): PromptLibraryFilter {
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    info<PromptLibraryFilter>("Loading prompt library filter from: $path")
                    return yamlMapper.readValue(file, PromptLibraryFilter::class.java)
                }
            }
            val runtimeFile = listOf(File(CONFIG_FILE_NAME), File("config/$CONFIG_FILE_NAME"))
                .firstOrNull { it.exists() }
            if (runtimeFile != null) {
                info<PromptLibraryFilter>("Loading prompt library filter from: ${runtimeFile.path}")
                return yamlMapper.readValue(runtimeFile, PromptLibraryFilter::class.java)
            }
            val resource = PromptLibraryFilter::class.java.getResourceAsStream("prompt-library.yaml")
            return if (resource != null) {
                resource.use { yamlMapper.readValue(it, PromptLibraryFilter::class.java) }
            } else {
                PromptLibraryFilter()
            }
        }
    }
}

/** Convert a glob-style [pattern] to a [Regex] (`*` = any sequence, `?` = single char). Case-insensitive. */
private fun globToRegex(pattern: String): Regex {
    val regexStr = buildString {
        for (char in pattern) {
            when (char) {
                '*' -> append(".*")
                '?' -> append(".")
                else -> append(Regex.escape(char.toString()))
            }
        }
    }
    return Regex(regexStr, RegexOption.IGNORE_CASE)
}
