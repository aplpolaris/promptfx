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
package tri.promptfx.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptArgType

class ArgViewConfigTest {

    @Test
    fun testArgViewConfig_prompt_enumeration() {
        val promptArgDef = PromptArgDef(
            name = "sentiment",
            description = "The sentiment classification",
            required = true,
            type = PromptArgType.enumeration,
            defaultValue = "neutral",
            allowedValues = listOf("positive", "negative", "neutral")
        )

        val argViewConfig = ArgViewConfig(promptArgDef, RuntimeArgConfig(
            fieldId = "sentiment",
            label = "Sentiment Analysis",
            control = RuntimeArgDisplayType.COMBO_BOX
        ))

        assertEquals("sentiment", argViewConfig.fieldId)
        assertEquals("Sentiment Analysis", argViewConfig.label)
        assertEquals("The sentiment classification", argViewConfig.description)
        assertEquals("neutral", argViewConfig.defaultValue)
        assertEquals(listOf("positive", "negative", "neutral"), argViewConfig.options)
        assertEquals("positive", argViewConfig.mode.value) // first option
    }

    @Test
    fun testArgViewConfig_config_enumeration() {
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
        assertEquals(listOf("JSON", "XML", "CSV", "YAML"), argViewConfig.options)
        assertEquals("JSON", argViewConfig.mode.value)
    }

    @Test
    fun testArgViewConfig_missing_argDef() {
        val runtimeConfig = RuntimeArgConfig(
            fieldId = "custom-field",
            label = "Custom Field",
            values = listOf("option1", "option2", "option3")
        )

        val argViewConfig = ArgViewConfig(null, runtimeConfig)

        assertEquals("custom-field", argViewConfig.fieldId)
        assertEquals("Custom Field", argViewConfig.label)
        assertNull(argViewConfig.description)
        assertEquals("option1", argViewConfig.defaultValue)
        assertEquals(listOf("option1", "option2", "option3"), argViewConfig.options)
        assertEquals("option1", argViewConfig.mode.value)
    }

}
