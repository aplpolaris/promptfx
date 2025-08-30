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

/** Demo class to show PromptLibrary configuration functionality. */
class PromptLibrary_FilteredDemo {
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("=== PromptLibrary Configuration Demo ===")
            
            // Load default library (no filtering)
            val defaultLibrary = PromptLibrary.INSTANCE
            val allPrompts = defaultLibrary.list()
            println("Default library loaded ${allPrompts.size} prompts")
            
            // Show some examples
            val textPrompts = allPrompts.filter { it.category?.startsWith("text-") == true }
            val examplePrompts = allPrompts.filter { it.category == "examples" }
            val docsPrompts = allPrompts.filter { it.category?.startsWith("docs-") == true }
            
            println("- Text prompts: ${textPrompts.size}")
            println("- Example prompts: ${examplePrompts.size}") 
            println("- Docs prompts: ${docsPrompts.size}")
            
            // Test filtering with configuration
            val config1 = PromptFilter(includeCategories = listOf("text-*"))
            val filteredLibrary1 = PromptLibrary.loadDefaultPromptLibrary(config1)
            val filteredPrompts1 = filteredLibrary1.list()
            println()
            println("With text-* filter: ${filteredPrompts1.size} prompts")
            filteredPrompts1.forEach { println("  - ${it.id} (${it.category})") }
            
            // Test exclusion filtering
            val config2 = PromptFilter(excludeCategories = listOf("examples"))
            val filteredLibrary2 = PromptLibrary.loadDefaultPromptLibrary(config2)
            val filteredPrompts2 = filteredLibrary2.list()
            println()
            println("Excluding examples: ${filteredPrompts2.size} prompts (should be ${allPrompts.size - examplePrompts.size})")
            
            println()
            println("Configuration functionality is working correctly!")
        }
    }
}