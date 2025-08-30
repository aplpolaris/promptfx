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
import tri.ai.prompt.resolved
import java.io.File
import java.nio.file.Path

/** Test to validate the complete Create Prompt workflow as specified in issue #511. */
class Issue511WorkflowTest {

    @Test
    fun testCreatePromptWorkflow(@TempDir tempDir: Path) {
        // Step 1: Simulate creating a new PromptDef object (as done by the dialog)
        val newPrompt = PromptDef(
            id = "custom/issue-511-test@1.0.0",
            name = "Issue 511 Test",
            title = "Create Prompt Feature Test",
            description = "Testing the create prompt dialog feature from issue #511",
            template = "Hello {{user}}, please {{action}} the following: {{content}}"
        )
        
        // Step 2: Save the prompt to custom-prompts.yaml (simulate dialog save action)
        val customFile = tempDir.resolve("custom-prompts.yaml").toFile()
        savePromptToFile(newPrompt, customFile)
        
        // Step 3: Verify the prompt was saved correctly
        val savedGroup = PromptGroupIO.readFromFile(customFile.toPath())
        assertEquals(1, savedGroup.prompts.size)
        val savedPrompt = savedGroup.prompts[0]
        assertEquals(newPrompt.id, savedPrompt.id)
        assertEquals(newPrompt.template, savedPrompt.template)
        
        // Step 4: Simulate refreshing the prompt list (as done by onCreate method)
        // In the real app, this would call PromptLibrary.refreshRuntimePrompts()
        // and update the UI list
        val refreshedLibrary = PromptLibrary().apply {
            addGroup(savedGroup.resolved())
        }
        
        // Step 5: Verify the new prompt can be found in the refreshed library
        val foundPrompt = refreshedLibrary.get(newPrompt.id)
        assertNotNull(foundPrompt)
        assertEquals(newPrompt.id, foundPrompt!!.id)
        assertEquals(newPrompt.template, foundPrompt.template)
        
        println("✓ Step 1: PromptDef object created successfully")
        println("✓ Step 2: Prompt saved to custom-prompts.yaml")
        println("✓ Step 3: Prompt verified in saved file")
        println("✓ Step 4: Library refresh simulated")
        println("✓ Step 5: New prompt found in refreshed library")
        
        // Additional test: verify the saved YAML has correct structure
        val yamlContent = customFile.readText()
        assertTrue(yamlContent.contains("groupId: \"custom\""))
        assertTrue(yamlContent.contains("id: \"custom/issue-511-test@1.0.0\""))
        assertTrue(yamlContent.contains("Hello {{user}}, please {{action}}"))
        
        println("✓ YAML structure validation passed")
    }
    
    @Test  
    fun testMultiplePromptCreation(@TempDir tempDir: Path) {
        val customFile = tempDir.resolve("custom-prompts.yaml").toFile()
        
        // Create first prompt
        val prompt1 = PromptDef(
            id = "custom/first@1.0.0",
            name = "First Prompt",
            template = "First template: {{input}}"
        )
        savePromptToFile(prompt1, customFile)
        
        // Create second prompt
        val prompt2 = PromptDef(
            id = "custom/second@1.0.0", 
            name = "Second Prompt",
            template = "Second template: {{data}}"
        )
        savePromptToFile(prompt2, customFile)
        
        // Verify both prompts are saved
        val savedGroup = PromptGroupIO.readFromFile(customFile.toPath())
        assertEquals(2, savedGroup.prompts.size)
        assertTrue(savedGroup.prompts.any { it.id == prompt1.id })
        assertTrue(savedGroup.prompts.any { it.id == prompt2.id })
        
        println("✓ Multiple prompt creation test passed")
    }
    
    private fun savePromptToFile(prompt: PromptDef, file: File) {
        // This mirrors the logic in CreatePromptDialog.savePromptToCustomFile()
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
            val initialGroup = PromptGroup("custom", prompts = emptyList())
            PromptGroupIO.MAPPER.writeValue(file, initialGroup)
        }
        
        val existingGroup = if (file.length() > 0) {
            try {
                PromptGroupIO.readFromFile(file.toPath())
            } catch (e: Exception) {
                PromptGroup("custom", prompts = emptyList())
            }
        } else {
            PromptGroup("custom", prompts = emptyList())
        }
        
        val updatedGroup = existingGroup.copy(
            prompts = existingGroup.prompts + prompt
        )
        
        PromptGroupIO.MAPPER.writeValue(file, updatedGroup)
    }
}