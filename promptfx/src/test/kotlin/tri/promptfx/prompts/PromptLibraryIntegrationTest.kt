/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.prompts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptGroup
import tri.ai.prompt.PromptGroupIO
import tri.ai.prompt.PromptLibrary
import java.io.File
import java.nio.file.Path

class PromptLibraryIntegrationTest {

    @Test
    fun testEndToEndPromptCreation(@TempDir tempDir: Path) {
        // Set up a temporary custom prompts file
        val customFile = tempDir.resolve("custom-prompts.yaml").toFile()
        
        // Create a test prompt
        val newPrompt = PromptDef(
            id = "custom/integration-test@1.0.0",
            name = "Integration Test Prompt",
            title = "Test Creation",
            description = "A prompt created through integration test",
            template = "This is a test template with {{variable}}"
        )
        
        // Simulate the save operation that CreatePromptDialog does
        savePromptToCustomFile(newPrompt, customFile)
        
        // Verify the prompt was saved correctly
        val savedGroup = PromptGroupIO.readFromFile(customFile.toPath())
        assertEquals(1, savedGroup.prompts.size)
        val savedPrompt = savedGroup.prompts[0]
        assertEquals(newPrompt.id, savedPrompt.id)
        assertEquals(newPrompt.name, savedPrompt.name)
        assertEquals(newPrompt.title, savedPrompt.title)
        assertEquals(newPrompt.description, savedPrompt.description)
        assertEquals(newPrompt.template, savedPrompt.template)
        
        // Test adding a second prompt
        val secondPrompt = PromptDef(
            id = "custom/second-test@1.0.0",
            name = "Second Test",
            template = "Another test template"
        )
        
        savePromptToCustomFile(secondPrompt, customFile)
        
        // Verify both prompts are present
        val updatedGroup = PromptGroupIO.readFromFile(customFile.toPath())
        assertEquals(2, updatedGroup.prompts.size)
        assertTrue(updatedGroup.prompts.any { it.id == newPrompt.id })
        assertTrue(updatedGroup.prompts.any { it.id == secondPrompt.id })
    }
    
    private fun savePromptToCustomFile(prompt: PromptDef, file: File) {
        // Ensure the file exists
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
            // Initialize with empty group
            val initialGroup = PromptGroup("custom", prompts = emptyList())
            PromptGroupIO.MAPPER.writeValue(file, initialGroup)
        }
        
        // Read existing content
        val existingGroup = if (file.length() > 0) {
            try {
                PromptGroupIO.readFromFile(file.toPath())
            } catch (e: Exception) {
                // If file is corrupted or empty, create a new group
                PromptGroup("custom", prompts = emptyList())
            }
        } else {
            PromptGroup("custom", prompts = emptyList())
        }
        
        // Add new prompt to existing prompts
        val updatedGroup = existingGroup.copy(
            prompts = existingGroup.prompts + prompt
        )
        
        // Write back to file
        PromptGroupIO.MAPPER.writeValue(file, updatedGroup)
    }
}