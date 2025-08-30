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
import tri.util.ui.MAPPER

class RuntimePromptViewConfigTest {

    @Test
    fun testArgConfigDisplayTypes() {
        // Test that all display types are supported
        val textAreaArg = ArgConfig(
            templateId = "input",
            label = "Input Text",
            description = "Enter your text here",
            displayType = ArgDisplayType.TEXT_AREA
        )
        
        val comboBoxArg = ArgConfig(
            id = "mode",
            templateId = "format",
            label = "Format",
            values = listOf("json", "xml", "csv"),
            displayType = ArgDisplayType.COMBO_BOX
        )
        
        val hiddenArg = ArgConfig(
            templateId = "hidden_param",
            label = "Hidden",
            defaultValue = "default_value",
            displayType = ArgDisplayType.HIDDEN
        )
        
        assertEquals(ArgDisplayType.TEXT_AREA, textAreaArg.displayType)
        assertEquals(ArgDisplayType.COMBO_BOX, comboBoxArg.displayType)
        assertEquals(ArgDisplayType.HIDDEN, hiddenArg.displayType)
        
        assertEquals("Enter your text here", textAreaArg.description)
        assertEquals("default_value", hiddenArg.defaultValue)
        assertNotNull(comboBoxArg.values)
        assertEquals(3, comboBoxArg.values!!.size)
    }

    @Test
    fun testRuntimePromptViewConfigParsing() {
        val yaml = """
            prompt:
              id: test/prompt
              category: Test
              title: Test Prompt
              description: A test prompt
            argOptions:
              - templateId: input
                label: Input Text
                description: Enter your text
                displayType: TEXT_AREA
              - templateId: format
                label: Format
                displayType: COMBO_BOX
                values: ["json", "xml"]
                defaultValue: "json"
              - templateId: hidden
                label: Hidden
                displayType: HIDDEN
                defaultValue: "secret"
            requestJson: true
            isShowModelParameters: true
        """.trimIndent()

        val config: RuntimePromptViewConfig = MAPPER.readValue(yaml, RuntimePromptViewConfig::class.java)
        
        assertEquals("test/prompt", config.promptDef.id)
        assertTrue(config.requestJson)
        assertTrue(config.isShowModelParameters)
        
        assertEquals(3, config.argOptions.size)
        
        val textAreaArg = config.argOptions[0]
        assertEquals("input", textAreaArg.templateId)
        assertEquals(ArgDisplayType.TEXT_AREA, textAreaArg.displayType)
        assertEquals("Enter your text", textAreaArg.description)
        
        val comboArg = config.argOptions[1]
        assertEquals("format", comboArg.templateId)
        assertEquals(ArgDisplayType.COMBO_BOX, comboArg.displayType)
        assertEquals(listOf("json", "xml"), comboArg.values)
        
        val hiddenArg = config.argOptions[2]
        assertEquals("hidden", hiddenArg.templateId)
        assertEquals(ArgDisplayType.HIDDEN, hiddenArg.displayType)
        assertEquals("secret", hiddenArg.defaultValue)
    }

    @Test
    fun testBackwardCompatibilityWithModeOptions() {
        val yamlWithModeOptions = """
            prompt:
              id: test/prompt
              category: Test
              title: Test Prompt
              description: A test prompt
            modeOptions:
              - id: entities
                templateId: mode
                label: Mode
              - templateId: format
                label: Format
                values: ["json", "xml"]
            isShowModelParameters: true
        """.trimIndent()

        val config: RuntimePromptViewConfig = MAPPER.readValue(yamlWithModeOptions, RuntimePromptViewConfig::class.java)
        
        assertEquals(0, config.argOptions.size)
        assertEquals(2, config.modeOptions.size)
        assertEquals(2, config.allArgOptions.size) // Combined list should have both
        
        val modeArg = config.allArgOptions[0]
        assertEquals("mode", modeArg.templateId)
        assertEquals(ArgDisplayType.COMBO_BOX, modeArg.displayType) // Default type
    }

    @Test
    fun testMixedArgOptionsAndModeOptions() {
        val yamlMixed = """
            prompt:
              id: test/prompt
              category: Test
              title: Test Prompt
              description: A test prompt
            argOptions:
              - templateId: input
                label: Input
                displayType: TEXT_AREA
            modeOptions:
              - templateId: mode
                label: Mode
                values: ["a", "b"]
        """.trimIndent()

        val config: RuntimePromptViewConfig = MAPPER.readValue(yamlMixed, RuntimePromptViewConfig::class.java)
        
        assertEquals(1, config.argOptions.size)
        assertEquals(1, config.modeOptions.size)
        assertEquals(2, config.allArgOptions.size)
        
        // argOptions should come first in combined list
        assertEquals(ArgDisplayType.TEXT_AREA, config.allArgOptions[0].displayType)
        assertEquals(ArgDisplayType.COMBO_BOX, config.allArgOptions[1].displayType)
    }
}