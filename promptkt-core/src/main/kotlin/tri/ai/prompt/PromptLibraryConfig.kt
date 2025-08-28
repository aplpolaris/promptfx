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

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Configuration for filtering prompts in the PromptLibrary.
 * Supports pattern-based filtering of prompts by ID and category.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class PromptLibraryConfig(
    /** Prompt ID patterns to include (if specified, only these will be loaded). */
    val includeIds: List<String> = emptyList(),
    /** Prompt category patterns to include (if specified, only these will be loaded). */
    val includeCategories: List<String> = emptyList(),
    /** Prompt ID patterns to exclude (applied after includes). */
    val excludeIds: List<String> = emptyList(),
    /** Prompt category patterns to exclude (applied after includes). */
    val excludeCategories: List<String> = emptyList()
) {
    
    /**
     * Check if a prompt should be included based on this configuration.
     * @param prompt The prompt to check
     * @return true if the prompt should be included, false otherwise
     */
    fun shouldInclude(prompt: PromptDef): Boolean {
        // Apply includes first - if any includes are specified, prompt must match at least one
        val hasIdIncludes = includeIds.isNotEmpty()
        val hasCategoryIncludes = includeCategories.isNotEmpty()
        val hasIncludes = hasIdIncludes || hasCategoryIncludes
        
        if (hasIncludes) {
            var matches = false
            
            // Check ID includes if specified
            if (hasIdIncludes) {
                matches = includeIds.any { pattern -> matchesPattern(prompt.id, pattern) }
            }
            
            // Check category includes if specified (OR condition with ID includes)
            if (hasCategoryIncludes && !matches) {
                matches = prompt.category != null && includeCategories.any { pattern -> matchesPattern(prompt.category, pattern) }
            }
            
            if (!matches) {
                return false
            }
        }
        
        // Apply excludes - if prompt matches any exclude pattern, it should not be included
        val idExcluded = excludeIds.any { pattern -> matchesPattern(prompt.id, pattern) }
        if (idExcluded) return false
        
        val categoryExcluded = prompt.category != null && 
            excludeCategories.any { pattern -> matchesPattern(prompt.category, pattern) }
        if (categoryExcluded) return false
        
        return true
    }
    
    /**
     * Check if a string matches a pattern. Supports simple wildcard matching with '*'.
     * @param text The text to match
     * @param pattern The pattern to match against
     * @return true if the text matches the pattern
     */
    private fun matchesPattern(text: String, pattern: String): Boolean {
        if (pattern == "*") return true
        if (!pattern.contains("*")) return text == pattern
        
        val regex = pattern.replace("*", ".*")
        return text.matches(Regex("^$regex$"))
    }
    
    companion object {
        /** Default configuration that includes all prompts. */
        val DEFAULT = PromptLibraryConfig()
    }
}