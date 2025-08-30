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
package tri.promptfx.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.PromptDef
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptArgType

class RuntimePromptViewConfigTest {

    @Test
    fun testRuntimePromptViewCreation() {
        val promptDef = PromptDef(
            id = "test/prompt",
            category = "Test",
            name = "Test Prompt",
            description = "A test prompt",
            template = "Test template with {{param}}: {{{input}}}"
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef
        )

        assertEquals("Test Prompt", config.prompt.title())
        assertEquals("A test prompt", config.prompt.description)
        assertEquals("test/prompt", config.prompt.id)
        assertTrue(config.args.isEmpty())
        assertTrue(config.userControls.prompt)
        assertFalse(config.userControls.modelParameters)
        assertFalse(config.userControls.multipleResponses)
        assertFalse(config.requestJson)
    }

    @Test
    fun testPromptDefOverrides() {
        val baseDef = PromptDef(
            id = "base/prompt",
            category = "Base",
            name = "Base Prompt",
            template = "Base template"
        )

        val overrideDef = PromptDef(
            id = "override/prompt", 
            category = "Override",
            name = "Override Prompt",
            description = "Override description",
            template = "Override template: {{{input}}}"
        )

        val config = RuntimePromptViewConfig(
            promptDef = overrideDef
        )

        // Test that the prompt property correctly merges global + override
        // (This tests the lazy initialization in RuntimePromptViewConfig)
        assertEquals("Override Prompt", config.prompt.name)
        assertEquals("Override", config.prompt.category)
        assertEquals("Override description", config.prompt.description)
        assertEquals("Override template: {{{input}}}", config.prompt.template)
    }

    @Test 
    fun testDescriptionFallback() {
        // Test fallback description when none provided
        val promptDef = PromptDef(
            id = "test/no-desc",
            category = "Test",
            name = "No Description Prompt",
            template = "Template without description"
        )

        val config = RuntimePromptViewConfig(promptDef = promptDef)
        
        // The config itself will have null description, but the RuntimePromptView will handle fallback
        // This is just testing that the config is properly constructed with null description
        assertNull(config.prompt.description)
        assertEquals("No Description Prompt", config.prompt.name)
        assertEquals("test/no-desc", config.prompt.id)
    }

    @Test
    fun testRuntimeArgConfigDefaults() {
        val argConfig = RuntimeArgConfig(
            fieldId = "test-field",
            label = "Test Label"
        )

        assertEquals("test-field", argConfig.fieldId)
        assertEquals("Test Label", argConfig.label)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, argConfig.control) // default
        assertNull(argConfig.modeId)
        assertNull(argConfig.values)
    }

    @Test
    fun testRuntimeArgConfigAllOptions() {
        val argConfig = RuntimeArgConfig(
            fieldId = "complex-field",
            control = RuntimeArgDisplayType.TEXT_AREA,
            modeId = "test-mode",
            label = "Complex Label",
            values = listOf("option1", "option2", "option3")
        )

        assertEquals("complex-field", argConfig.fieldId)
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, argConfig.control)
        assertEquals("test-mode", argConfig.modeId)
        assertEquals("Complex Label", argConfig.label)
        assertEquals(listOf("option1", "option2", "option3"), argConfig.values)
    }

    @Test
    fun testRuntimeUserControlsDefaults() {
        val controls = RuntimeUserControls()
        
        assertTrue(controls.prompt) // default true
        assertFalse(controls.modelParameters) // default false  
        assertFalse(controls.multipleResponses) // default false
    }

    @Test
    fun testRuntimeUserControlsCustom() {
        val controls = RuntimeUserControls(
            prompt = false,
            modelParameters = true,
            multipleResponses = true
        )
        
        assertFalse(controls.prompt)
        assertTrue(controls.modelParameters)  
        assertTrue(controls.multipleResponses)
    }

    @Test
    fun testCompleteViewConfig() {
        val promptDef = PromptDef(
            id = "complete/test",
            category = "Complete",
            name = "Complete Test",
            description = "Complete configuration test",
            template = "Extract {{format}} from {{mode}}: {{{input}}}\n\nGuidance: {{guidance}}",
            args = listOf(
                PromptArgDef("input", "The input text", true, PromptArgType.string),
                PromptArgDef("format", "Output format", false, PromptArgType.string, "JSON", 
                    listOf("JSON", "XML", "CSV"))
            )
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            args = listOf(
                RuntimeArgConfig(
                    fieldId = "format",
                    label = "Output Format",
                    values = listOf("JSON", "XML", "CSV", "YAML")
                ),
                RuntimeArgConfig(
                    fieldId = "mode",
                    modeId = "extraction-modes", 
                    label = "Extraction Mode"
                ),
                RuntimeArgConfig(
                    fieldId = "input",
                    control = RuntimeArgDisplayType.TEXT_AREA,
                    label = "Source Text"
                ),
                RuntimeArgConfig(
                    fieldId = "guidance",
                    control = RuntimeArgDisplayType.TEXT_AREA,
                    label = "Additional Guidance"
                )
            ),
            userControls = RuntimeUserControls(
                prompt = true,
                modelParameters = true,
                multipleResponses = false
            ),
            requestJson = true
        )

        // Test basic config
        assertEquals("Complete Test", config.prompt.name)
        assertEquals(4, config.args.size)
        assertTrue(config.userControls.modelParameters)
        assertTrue(config.requestJson)

        // Test arg configurations
        val formatArg = config.args.find { it.fieldId == "format" }
        assertNotNull(formatArg)
        assertEquals(listOf("JSON", "XML", "CSV", "YAML"), formatArg?.values)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, formatArg?.control)

        val modeArg = config.args.find { it.fieldId == "mode" }
        assertNotNull(modeArg)
        assertEquals("extraction-modes", modeArg?.modeId)

        val textAreaArgs = config.args.filter { it.control == RuntimeArgDisplayType.TEXT_AREA }
        assertEquals(2, textAreaArgs.size)
        assertTrue(textAreaArgs.any { it.fieldId == "input" })
        assertTrue(textAreaArgs.any { it.fieldId == "guidance" })
    }

    @Test
    fun testAllRuntimeArgDisplayTypes() {
        val displayTypes = RuntimeArgDisplayType.values()
        
        assertEquals(3, displayTypes.size)
        
        // Verify all expected types are present
        val typeNames = displayTypes.map { it.name }
        assertTrue(typeNames.contains("COMBO_BOX"))
        assertTrue(typeNames.contains("TEXT_AREA")) 
        assertTrue(typeNames.contains("HIDDEN"))
    }
}