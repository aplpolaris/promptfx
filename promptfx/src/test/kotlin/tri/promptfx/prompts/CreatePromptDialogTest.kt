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
import java.nio.file.Path

class CreatePromptDialogTest {

    @Test
    fun testSavePromptToCustomFile(@TempDir tempDir: Path) {
        val customFile = tempDir.resolve("custom-prompts.yaml").toFile()
        
        val testPrompt = PromptDef(
            id = "test/sample@1.0.0",
            name = "Sample Prompt",
            title = "Test Sample",
            description = "A test prompt for validation",
            template = "Hello {{name}}!"
        )
        val initialGroup = PromptGroup("custom", prompts = emptyList())
        PromptGroupIO.MAPPER.writeValue(customFile, initialGroup)
        
        val existingGroup = PromptGroupIO.readFromFile(customFile.toPath())
        val updatedGroup = existingGroup.copy(
            prompts = existingGroup.prompts + testPrompt
        )
        PromptGroupIO.MAPPER.writeValue(customFile, updatedGroup)
        
        // Verify the prompt was saved correctly
        val savedGroup = PromptGroupIO.readFromFile(customFile.toPath())
        assertEquals(1, savedGroup.prompts.size)
        assertEquals("test/sample@1.0.0", savedGroup.prompts[0].id)
        assertEquals("Sample Prompt", savedGroup.prompts[0].name)
        assertEquals("Hello {{name}}!", savedGroup.prompts[0].template)
    }

}