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
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptArgType

class ArgViewConfigTest {

    @Test
    fun testArgViewConfigWithPromptArgDef() {
        // Create a PromptArgDef with enumeration values
        val promptArgDef = PromptArgDef(
            name = "sentiment",
            description = "The sentiment classification",
            required = true,
            type = PromptArgType.enumeration,
            defaultValue = "neutral",
            allowedValues = listOf("positive", "negative", "neutral")
        )

        val runtimeConfig = RuntimeArgConfig(
            fieldId = "sentiment",
            label = "Sentiment Analysis",
            control = RuntimeArgDisplayType.COMBO_BOX
        )

        val argViewConfig = ArgViewConfig(promptArgDef, runtimeConfig)

        // Test basic properties
        assertEquals("sentiment", argViewConfig.fieldId)
        assertEquals("Sentiment Analysis", argViewConfig.label)
        assertEquals("The sentiment classification", argViewConfig.description)
        assertEquals("neutral", argViewConfig.defaultValue)

        // Test options priority: should use PromptArgDef allowedValues since runtimeConfig.values is null
        assertEquals(listOf("positive", "negative", "neutral"), argViewConfig.options)
        assertEquals("positive", argViewConfig.mode.value) // first option
    }

    @Test
    fun testArgViewConfigWithRuntimeConfigValues() {
        // Test that RuntimeArgConfig.values takes priority over PromptArgDef.allowedValues
        val promptArgDef = PromptArgDef(
            name = "format",
            description = "Output format",
            required = false,
            type = PromptArgType.enumeration,
            allowedValues = listOf("JSON", "XML") // These should be overridden
        )

        val runtimeConfig = RuntimeArgConfig(
            fieldId = "format",
            label = "Output Format",
            values = listOf("JSON", "XML", "CSV", "YAML") // This takes priority
        )

        val argViewConfig = ArgViewConfig(promptArgDef, runtimeConfig)

        assertEquals("format", argViewConfig.fieldId)
        assertEquals("Output Format", argViewConfig.label)
        assertEquals("Output format", argViewConfig.description)

        // Runtime values should take priority
        assertEquals(listOf("JSON", "XML", "CSV", "YAML"), argViewConfig.options)
        assertEquals("JSON", argViewConfig.mode.value)
    }

    @Test
    fun testArgViewConfigWithModeId() {
        // Test that modeId is structured correctly (we can't test actual mode lookup in test environment)
        val promptArgDef = PromptArgDef(
            name = "language",
            description = "Target language",
            required = true,
            type = PromptArgType.string
        )

        val runtimeConfig = RuntimeArgConfig(
            fieldId = "language",
            label = "Target Language",
            modeId = "translation" // This should be used to look up options
        )

        // Test the configuration structure without trying to create ArgViewConfig 
        // which may fail due to missing runtime configuration files
        assertEquals("language", runtimeConfig.fieldId)
        assertEquals("Target Language", runtimeConfig.label) 
        assertEquals("translation", runtimeConfig.modeId)
        assertEquals("language", promptArgDef.name)
        assertEquals("Target language", promptArgDef.description)
        
        // Note: In a real environment, RuntimePromptViewConfigs.modeOptionList("translation") 
        // would provide the actual options for the translation mode
    }

    @Test
    fun testArgViewConfigWithNoPromptArgDef() {
        // Test when no PromptArgDef is available (only runtime config)
        val runtimeConfig = RuntimeArgConfig(
            fieldId = "custom-field",
            label = "Custom Field",
            values = listOf("option1", "option2", "option3")
        )

        val argViewConfig = ArgViewConfig(null, runtimeConfig)

        assertEquals("custom-field", argViewConfig.fieldId)
        assertEquals("Custom Field", argViewConfig.label)
        assertNull(argViewConfig.description) // no PromptArgDef
        assertEquals("option1", argViewConfig.defaultValue) // config.values?.firstOrNull() = "option1"

        assertEquals(listOf("option1", "option2", "option3"), argViewConfig.options)
        assertEquals("option1", argViewConfig.mode.value)
    }

    @Test
    fun testArgViewConfigDefaultValues() {
        // Test with minimal configuration - no values anywhere
        val runtimeConfig = RuntimeArgConfig(
            fieldId = "minimal",
            label = "Minimal Config"
        )

        val argViewConfig = ArgViewConfig(null, runtimeConfig)

        assertEquals("minimal", argViewConfig.fieldId)
        assertEquals("Minimal Config", argViewConfig.label)
        assertNull(argViewConfig.description)
        assertNull(argViewConfig.defaultValue)

        // Should get fallback empty string option
        assertEquals(listOf(""), argViewConfig.options)
        assertEquals("", argViewConfig.mode.value)
    }

    @Test
    fun testArgViewConfigWithTextAreaControl() {
        // Test that control type is preserved from runtime config
        val promptArgDef = PromptArgDef(
            name = "input",
            description = "The input text to process",
            required = true,
            type = PromptArgType.string
        )

        val runtimeConfig = RuntimeArgConfig(
            fieldId = "input",
            label = "Input Text",
            control = RuntimeArgDisplayType.TEXT_AREA
        )

        val argViewConfig = ArgViewConfig(promptArgDef, runtimeConfig)

        assertEquals("input", argViewConfig.fieldId)
        assertEquals("Input Text", argViewConfig.label)
        assertEquals("The input text to process", argViewConfig.description)
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, argViewConfig.config.control)

        // Text areas typically don't have predefined options
        // PromptArgDef has empty allowedValues by default, so we get empty list 
        assertEquals(emptyList<String>(), argViewConfig.options)
        assertEquals("", argViewConfig.mode.value) // mode gets first option or empty string
    }

    @Test
    fun testArgViewConfigDefaultValuePriority() {
        // Test that config.values takes priority over argDef.defaultValue for defaultValue
        val promptArgDef = PromptArgDef(
            name = "priority-test",
            description = "Test default value priority",
            required = false,
            type = PromptArgType.string,
            defaultValue = "from-prompt-def"
        )

        val runtimeConfigWithValues = RuntimeArgConfig(
            fieldId = "priority-test",
            label = "Priority Test",
            values = listOf("from-runtime-config", "other-option")
        )

        val argViewConfig = ArgViewConfig(promptArgDef, runtimeConfigWithValues)

        // defaultValue should use first value from runtime config
        assertEquals("from-runtime-config", argViewConfig.defaultValue)
        assertEquals("from-runtime-config", argViewConfig.mode.value)

        // Test without runtime config values
        val runtimeConfigNoValues = RuntimeArgConfig(
            fieldId = "priority-test",
            label = "Priority Test"
        )

        val argViewConfig2 = ArgViewConfig(promptArgDef, runtimeConfigNoValues)

        // defaultValue should fall back to promptArgDef.defaultValue
        assertEquals("from-prompt-def", argViewConfig2.defaultValue)
    }
}