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