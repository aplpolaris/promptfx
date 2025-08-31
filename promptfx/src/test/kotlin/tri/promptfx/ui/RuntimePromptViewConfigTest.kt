package tri.promptfx.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.PromptArgDef
import tri.ai.prompt.PromptArgType
import tri.ai.prompt.PromptDef

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
    fun testCompleteViewConfig() {
        val promptDef = PromptDef(
            id = "complete/test",
            category = "Complete",
            name = "Complete Test",
            description = "Complete configuration test",
            template = "Extract {{format}} from {{mode}}: {{{input}}}\n\nGuidance: {{guidance}}",
            args = listOf(
                PromptArgDef("input", "The input text", true, PromptArgType.string),
                PromptArgDef("format", "Output format", false, PromptArgType.string, "JSON", listOf("JSON", "XML", "CSV"))
            )
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            args = listOf(
                RuntimeArgConfig(fieldId = "format", label = "Output Format", values = listOf("JSON", "XML", "CSV", "YAML")),
                RuntimeArgConfig(fieldId = "mode", modeId = "extraction-modes", label = "Extraction Mode"),
                RuntimeArgConfig(fieldId = "input", control = RuntimeArgDisplayType.TEXT_AREA, label = "Source Text"),
                RuntimeArgConfig(fieldId = "guidance", control = RuntimeArgDisplayType.TEXT_AREA, label = "Additional Guidance")
            ),
            userControls = RuntimeUserControls(prompt = true, modelParameters = true, multipleResponses = false),
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



}