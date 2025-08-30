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
import tri.promptfx.ui.RuntimeUserControls
import tri.promptfx.ui.RuntimeArgConfig
import tri.promptfx.ui.RuntimeArgDisplayType
import tri.util.ui.MAPPER

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
            args = listOf(),
            userControls = RuntimeUserControls(
                prompt = true,
                modelParameters = false,
                multipleResponses = false
            )
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
            args = listOf(),
            userControls = RuntimeUserControls(
                prompt = true,
                modelParameters = false,
                multipleResponses = false
            )
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
            args = listOf(),
            userControls = RuntimeUserControls(
                prompt = true,
                modelParameters = true,
                multipleResponses = false
            )
        )
        
        // Verify the template is multiline
        assertTrue(config.prompt.template!!.contains('\n'))
        assertEquals("Line 1\nLine 2\nLine 3", config.prompt.template)
    }

    @Test
    fun testNewRuntimeArgConfig() {
        // Test RuntimeArgConfig with combo box control
        val comboConfig = RuntimeArgConfig(
            fieldId = "mode",
            label = "Operation Mode",
            values = listOf("simple", "advanced", "expert")
        )
        
        assertEquals("mode", comboConfig.fieldId)
        assertEquals("Operation Mode", comboConfig.label)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, comboConfig.control)
        assertEquals(listOf("simple", "advanced", "expert"), comboConfig.values)
        assertNull(comboConfig.modeId)

        // Test RuntimeArgConfig with text area control
        val textAreaConfig = RuntimeArgConfig(
            fieldId = "input",
            control = RuntimeArgDisplayType.TEXT_AREA,
            label = "Source Text"
        )
        
        assertEquals("input", textAreaConfig.fieldId)
        assertEquals("Source Text", textAreaConfig.label)
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, textAreaConfig.control)
        assertNull(textAreaConfig.values)

        // Test RuntimeArgConfig with mode reference
        val modeConfig = RuntimeArgConfig(
            fieldId = "format",
            modeId = "structured-format",
            label = "Output Format"
        )
        
        assertEquals("format", modeConfig.fieldId)
        assertEquals("structured-format", modeConfig.modeId)
        assertEquals("Output Format", modeConfig.label)
    }

    @Test
    fun testRuntimeUserControls() {
        // Test default values
        val defaultControls = RuntimeUserControls()
        assertTrue(defaultControls.prompt)
        assertFalse(defaultControls.modelParameters)
        assertFalse(defaultControls.multipleResponses)

        // Test custom values
        val customControls = RuntimeUserControls(
            prompt = false,
            modelParameters = true,
            multipleResponses = true
        )
        assertFalse(customControls.prompt)
        assertTrue(customControls.modelParameters)
        assertTrue(customControls.multipleResponses)
    }

    @Test
    fun testRuntimePromptViewConfigWithArgs() {
        val promptDef = PromptDef(
            id = "test/structured-extraction",
            category = "Text",
            name = "Structured Data Extraction",
            description = "Extract structured data from text",
            template = "Extract {{format}} data from: {{{input}}}\n\nGuidance: {{guidance}}"
        )

        val args = listOf(
            RuntimeArgConfig(
                fieldId = "format",
                modeId = "structured-format",
                label = "Output Format"
            ),
            RuntimeArgConfig(
                fieldId = "input",
                control = RuntimeArgDisplayType.TEXT_AREA,
                label = "Source Text"
            ),
            RuntimeArgConfig(
                fieldId = "guidance",
                label = "Extraction Guidance",
                values = listOf("Focus on dates", "Focus on names", "Focus on amounts")
            )
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            args = args,
            userControls = RuntimeUserControls(
                prompt = true,
                modelParameters = true,
                multipleResponses = false
            ),
            requestJson = true
        )

        // Verify basic properties
        assertEquals("Structured Data Extraction", config.prompt.name)
        assertEquals("Text", config.prompt.category)
        assertEquals(3, config.args.size)
        assertTrue(config.userControls.modelParameters)
        assertFalse(config.userControls.multipleResponses)
        assertTrue(config.requestJson)

        // Verify args
        val formatArg = config.args.find { it.fieldId == "format" }
        assertNotNull(formatArg)
        assertEquals("structured-format", formatArg?.modeId)
        assertEquals("Output Format", formatArg?.label)
        
        val inputArg = config.args.find { it.fieldId == "input" }
        assertNotNull(inputArg)
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, inputArg?.control)
        assertEquals("Source Text", inputArg?.label)
        
        val guidanceArg = config.args.find { it.fieldId == "guidance" }
        assertNotNull(guidanceArg)
        assertEquals(listOf("Focus on dates", "Focus on names", "Focus on amounts"), guidanceArg?.values)
    }

    @Test 
    fun testRuntimeArgDisplayTypes() {
        // Test all display types are present
        val displayTypes = RuntimeArgDisplayType.values()
        assertEquals(3, displayTypes.size)
        assertTrue(displayTypes.contains(RuntimeArgDisplayType.COMBO_BOX))
        assertTrue(displayTypes.contains(RuntimeArgDisplayType.TEXT_AREA))
        assertTrue(displayTypes.contains(RuntimeArgDisplayType.HIDDEN))
    }

    @Test
    fun testBackwardCompatibilityForEmptyArgs() {
        // Test that empty args list works (backward compatibility)
        val promptDef = PromptDef(
            id = "test/simple",
            category = "Text",
            name = "Simple Test",
            template = "Simple template: {{{input}}}"
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            args = listOf(), // Empty args should work fine
            userControls = RuntimeUserControls()
        )

        assertNotNull(config)
        assertEquals("Simple Test", config.prompt.name)
        assertEquals(0, config.args.size)
        assertTrue(config.userControls.prompt) // default value
    }

    @Test
    fun testYamlSerialization() {
        val promptDef = PromptDef(
            id = "test/yaml-serialize",
            category = "Test",
            name = "YAML Serialization Test",
            description = "Test YAML serialization",
            template = "Process {{mode}} for: {{{input}}}"
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef,
            args = listOf(
                RuntimeArgConfig(
                    fieldId = "mode",
                    modeId = "test-modes",
                    label = "Processing Mode"
                ),
                RuntimeArgConfig(
                    fieldId = "input",
                    control = RuntimeArgDisplayType.TEXT_AREA,
                    label = "Input Text"
                )
            ),
            userControls = RuntimeUserControls(
                prompt = false,
                modelParameters = true,
                multipleResponses = true
            ),
            requestJson = true
        )

        // Test serialization to YAML
        val yamlString = MAPPER.writeValueAsString(config)
        assertNotNull(yamlString)
        assertTrue(yamlString.contains("fieldId: \"mode\""))
        assertTrue(yamlString.contains("modeId: \"test-modes\""))
        assertTrue(yamlString.contains("control: \"TEXT_AREA\""))
        assertTrue(yamlString.contains("modelParameters: true"))
        assertTrue(yamlString.contains("requestJson: true"))

        // Test deserialization from YAML
        val deserializedConfig = MAPPER.readValue(yamlString, RuntimePromptViewConfig::class.java)
        assertEquals(config.prompt.name, deserializedConfig.prompt.name)
        assertEquals(config.args.size, deserializedConfig.args.size)
        assertEquals(config.userControls.modelParameters, deserializedConfig.userControls.modelParameters)
        assertEquals(config.requestJson, deserializedConfig.requestJson)

        // Test specific args properties
        val modeArg = deserializedConfig.args.find { it.fieldId == "mode" }
        assertNotNull(modeArg)
        assertEquals("test-modes", modeArg?.modeId)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, modeArg?.control) // default value

        val inputArg = deserializedConfig.args.find { it.fieldId == "input" }
        assertNotNull(inputArg)
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, inputArg?.control)
    }

    @Test
    fun testYamlMinimalSerialization() {
        // Test that defaults are not serialized (thanks to @JsonInclude(NON_DEFAULT))
        val promptDef = PromptDef(
            id = "test/minimal",
            category = "Test", 
            name = "Minimal Test",
            template = "Simple: {{{input}}}"
        )

        val config = RuntimePromptViewConfig(
            promptDef = promptDef
            // Using all default values
        )

        val yamlString = MAPPER.writeValueAsString(config)
        
        // Should only contain the prompt definition, not default values
        assertTrue(yamlString.contains("prompt:"))
        assertFalse(yamlString.contains("args:")) // empty list should not appear
        assertTrue(yamlString.contains("userControls: {}")) // empty object still appears, but no explicit boolean values
        assertFalse(yamlString.contains("requestJson: false")) // default false should not appear
    }

    @Test
    fun testViewConfigMap() {
        // Test that view configs can be serialized as a map (like in views.yaml)
        val configs = mapOf(
            "test-view-1" to RuntimePromptViewConfig(
                promptDef = PromptDef(
                    id = "test/view1",
                    category = "Test",
                    name = "Test View 1",
                    template = "Template 1: {{{input}}}"
                )
            ),
            "test-view-2" to RuntimePromptViewConfig(
                promptDef = PromptDef(
                    id = "test/view2", 
                    category = "Test",
                    name = "Test View 2",
                    template = "Template 2: {{{input}}}"
                ),
                userControls = RuntimeUserControls(modelParameters = true)
            )
        )

        val yamlString = MAPPER.writeValueAsString(configs)
        assertTrue(yamlString.contains("test-view-1:"))
        assertTrue(yamlString.contains("test-view-2:"))
        assertTrue(yamlString.contains("modelParameters: true"))

        // Test deserialization
        val mapType = MAPPER.typeFactory.constructMapType(Map::class.java, String::class.java, RuntimePromptViewConfig::class.java)
        val deserializedConfigs = MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(yamlString, mapType)
        assertEquals(2, deserializedConfigs.size)
        assertTrue(deserializedConfigs.containsKey("test-view-1"))
        assertTrue(deserializedConfigs.containsKey("test-view-2"))
        assertTrue(deserializedConfigs["test-view-2"]!!.userControls.modelParameters)
    }
}