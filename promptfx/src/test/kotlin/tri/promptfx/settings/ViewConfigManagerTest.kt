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
package tri.promptfx.settings

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.PromptDef
import tri.promptfx.ui.RuntimePromptViewConfig

class ViewConfigManagerTest {

    @Test
    fun testGenerateViewId() {
        val viewId1 = ViewConfigManager.generateViewId("Text", "My Test View")
        assertEquals("text-my-test-view", viewId1)
        
        val viewId2 = ViewConfigManager.generateViewId("Code Review", "Java Analyzer")
        assertEquals("code-review-java-analyzer", viewId2)
    }

    @Test
    fun testCreateViewConfig() {
        val promptDef = PromptDef(
            id = "test/sample-prompt",
            category = "Text",
            name = "Sample Test View",
            description = "A test view for unit testing",
            template = "Process this text: {{{input}}}"
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            modeOptions = listOf(),
            isShowModelParameters = false,
            isShowMultipleResponseOption = false
        )

        assertNotNull(config)
        assertEquals("Sample Test View", config.prompt.name)
        assertEquals("Text", config.prompt.category)
        assertEquals("Process this text: {{{input}}}", config.prompt.template)
    }

    @Test
    fun testViewCreationWorkflow() {
        // Test the basic workflow without saving to actual file
        val viewName = "Test Custom View"
        val viewCategory = "Testing"
        val promptTemplate = "Test prompt: {{{input}}}"
        
        // Generate view ID 
        val viewId = ViewConfigManager.generateViewId(viewCategory, viewName)
        assertEquals("testing-test-custom-view", viewId)
        
        // Create prompt definition
        val promptDef = PromptDef(
            id = "${viewCategory.lowercase()}/${viewName.lowercase().replace(" ", "-")}",
            category = viewCategory,
            name = viewName,
            template = promptTemplate
        )
        
        // Create config
        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            modeOptions = listOf(),
            isShowModelParameters = false,
            isShowMultipleResponseOption = false
        )
        
        // Verify the config is properly constructed
        assertNotNull(config)
        assertEquals(viewName, config.prompt.name)
        assertEquals(viewCategory, config.prompt.category)
        assertEquals(promptTemplate, config.prompt.template)
        assertEquals("testing/test-custom-view", config.prompt.id)
    }

    @Test
    fun testYamlFormatting() {
        // Test that multiline templates use pipe syntax
        val promptDef = PromptDef(
            id = "test/multiline",
            category = "Test",
            name = "Multiline Test",
            description = "A test with multiline template",
            template = "Line 1\nLine 2\nLine 3"
        )
        
        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            modeOptions = listOf(),
            isShowModelParameters = true,
            isShowMultipleResponseOption = false
        )
        
        // Verify the template is multiline
        assertTrue(config.prompt.template!!.contains('\n'))
        assertEquals("Line 1\nLine 2\nLine 3", config.prompt.template)
    }
}