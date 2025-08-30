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

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.prompt.PromptDef
import tri.util.ui.MAPPER
import tri.util.ui.WorkspaceViewAffordance

class RuntimePromptViewConfigTest {

    @Test
    fun `test RuntimePromptViewConfig with default values`() {
        val promptDef = PromptDef(
            id = "test/basic",
            category = "Test", 
            name = "Basic Test",
            template = "Process: {{{input}}}"
        )
        
        val config = RuntimePromptViewConfig(promptDef = promptDef)
        
        assertEquals(promptDef, config.promptDef)
        assertEquals(emptyList<RuntimeArgConfig>(), config.args)
        assertEquals(RuntimeUserControls(), config.userControls)
        assertEquals(false, config.requestJson)
        assertEquals(WorkspaceViewAffordance.INPUT_ONLY, config.affordances)
    }

    @Test
    fun `test RuntimePromptViewConfig with custom values`() {
        val promptDef = PromptDef(
            id = "test/custom",
            category = "Test",
            name = "Custom Test", 
            template = "Process {{mode}} for {{{input}}}"
        )
        
        val args = listOf(
            RuntimeArgConfig(
                fieldId = "mode",
                label = "Processing Mode",
                values = listOf("Fast", "Detailed")
            )
        )
        
        val userControls = RuntimeUserControls(
            prompt = false,
            modelParameters = true,
            multipleResponses = true
        )
        
        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            args = args,
            userControls = userControls,
            requestJson = true
        )
        
        assertEquals(promptDef, config.promptDef)
        assertEquals(args, config.args)
        assertEquals(userControls, config.userControls)
        assertEquals(true, config.requestJson)
    }

    @Test
    fun `test RuntimeArgConfig with combo box control`() {
        val argConfig = RuntimeArgConfig(
            fieldId = "mode",
            label = "Processing Mode",
            values = listOf("Option1", "Option2", "Option3")
        )
        
        assertEquals("mode", argConfig.fieldId)
        assertEquals("Processing Mode", argConfig.label)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, argConfig.control)
        assertEquals(listOf("Option1", "Option2", "Option3"), argConfig.values)
        assertNull(argConfig.modeId)
    }

    @Test
    fun `test RuntimeArgConfig with text area control`() {
        val argConfig = RuntimeArgConfig(
            fieldId = "instruct", 
            label = "Instructions",
            control = RuntimeArgDisplayType.TEXT_AREA
        )
        
        assertEquals("instruct", argConfig.fieldId)
        assertEquals("Instructions", argConfig.label)
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, argConfig.control)
        assertNull(argConfig.values)
        assertNull(argConfig.modeId)
    }

    @Test
    fun `test RuntimeArgConfig with mode reference`() {
        val argConfig = RuntimeArgConfig(
            fieldId = "format",
            label = "Output Format", 
            modeId = "structured-format"
        )
        
        assertEquals("format", argConfig.fieldId)
        assertEquals("Output Format", argConfig.label)
        assertEquals("structured-format", argConfig.modeId)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, argConfig.control)
    }

    @Test
    fun `test RuntimeUserControls default values`() {
        val userControls = RuntimeUserControls()
        
        assertEquals(true, userControls.prompt)
        assertEquals(false, userControls.modelParameters)
        assertEquals(false, userControls.multipleResponses)
    }

    @Test
    fun `test RuntimeUserControls custom values`() {
        val userControls = RuntimeUserControls(
            prompt = false,
            modelParameters = true,
            multipleResponses = true
        )
        
        assertEquals(false, userControls.prompt)
        assertEquals(true, userControls.modelParameters)
        assertEquals(true, userControls.multipleResponses)
    }

    @Test
    fun `test RuntimeArgDisplayType enum values`() {
        assertEquals(3, RuntimeArgDisplayType.values().size)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, RuntimeArgDisplayType.valueOf("COMBO_BOX"))
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, RuntimeArgDisplayType.valueOf("TEXT_AREA"))
        assertEquals(RuntimeArgDisplayType.HIDDEN, RuntimeArgDisplayType.valueOf("HIDDEN"))
    }

    @Test
    fun `test JSON serialization basic test`() {
        val promptDef = PromptDef(
            id = "test/serialization",
            category = "Test",
            name = "Serialization Test",
            template = "Process: {{{input}}}"
        )
        
        val config = RuntimePromptViewConfig(promptDef = promptDef)
        
        // Serialize to JSON
        val json = MAPPER.writeValueAsString(config)
        assertNotNull(json)
        assertTrue(json.contains("test/serialization"))
        
        // Deserialize back from JSON
        val deserializedConfig = MAPPER.readValue<RuntimePromptViewConfig>(json)
        assertEquals(config.promptDef.id, deserializedConfig.promptDef.id)
    }

    @Test
    fun `test prompt lazy evaluation`() {
        val promptDef = PromptDef(
            id = "test/lazy",
            category = "Test",
            name = "Lazy Test",
            template = "Original template"
        )
        
        val config = RuntimePromptViewConfig(promptDef = promptDef)
        
        // The prompt property should return the promptDef since no global exists
        assertEquals(promptDef.id, config.prompt.id)
        assertEquals(promptDef.template, config.prompt.template)
    }
}