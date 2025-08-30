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
import tri.util.ui.MAPPER

class RuntimeViewConfigYamlTest {

    @Test
    fun `test parsing view configuration from YAML`() {
        val yamlContent = """
            test-view:
              prompt:
                id: test/example
                category: Test
                name: Test Example
                template: |
                  Process {{mode}} for: {{{input}}}
              args:
                - fieldId: mode
                  label: Processing Mode
                  values:
                    - Fast
                    - Detailed
                - fieldId: input
                  label: Input Text
                  control: TEXT_AREA
              userControls:
                prompt: false
                modelParameters: true
                multipleResponses: true
              requestJson: true
        """.trimIndent()
        
        // Parse the YAML as a Map first
        val configMap = MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(yamlContent)
        
        assertNotNull(configMap)
        assertTrue(configMap.containsKey("test-view"))
        
        val config = configMap["test-view"]!!
        
        // Verify prompt definition
        assertEquals("test/example", config.promptDef.id)
        assertEquals("Test", config.promptDef.category)
        assertEquals("Test Example", config.promptDef.name)
        assertTrue(config.promptDef.template!!.contains("Process {{mode}} for: {{{input}}}"))
        
        // Verify args
        assertEquals(2, config.args.size)
        
        val modeArg = config.args.find { it.fieldId == "mode" }!!
        assertEquals("Processing Mode", modeArg.label)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, modeArg.control)
        assertEquals(listOf("Fast", "Detailed"), modeArg.values)
        
        val inputArg = config.args.find { it.fieldId == "input" }!!
        assertEquals("Input Text", inputArg.label)
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, inputArg.control)
        
        // Verify user controls
        assertEquals(false, config.userControls.prompt)
        assertEquals(true, config.userControls.modelParameters)
        assertEquals(true, config.userControls.multipleResponses)
        
        // Verify request JSON flag
        assertEquals(true, config.requestJson)
    }

    @Test
    fun `test parsing view configuration with mode references`() {
        val yamlContent = """
            sentiment-analysis:
              prompt:
                id: text-classify/sentiment
                category: Text
              args:
                - modeId: sentiment
                  fieldId: instruct
                  label: Sentiment
              userControls:
                modelParameters: true
                multipleResponses: true
        """.trimIndent()
        
        val configMap = MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(yamlContent)
        val config = configMap["sentiment-analysis"]!!
        
        assertEquals("text-classify/sentiment", config.promptDef.id)
        assertEquals("Text", config.promptDef.category)
        
        assertEquals(1, config.args.size)
        val arg = config.args[0]
        assertEquals("sentiment", arg.modeId)
        assertEquals("instruct", arg.fieldId)
        assertEquals("Sentiment", arg.label)
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, arg.control)
    }

    @Test
    fun `test parsing view configuration with minimal settings`() {
        val yamlContent = """
            simple-view:
              prompt:
                id: simple/test
                category: Simple
                name: Simple Test
                template: "{{{input}}}"
        """.trimIndent()
        
        val configMap = MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(yamlContent)
        val config = configMap["simple-view"]!!
        
        assertEquals("simple/test", config.promptDef.id)
        assertEquals("Simple", config.promptDef.category)
        assertEquals("Simple Test", config.promptDef.name)
        assertEquals("{{{input}}}", config.promptDef.template)
        
        // Verify defaults
        assertEquals(emptyList<RuntimeArgConfig>(), config.args)
        assertEquals(RuntimeUserControls(), config.userControls)
        assertEquals(false, config.requestJson)
    }

    @Test
    fun `test parsing multiple view configurations`() {
        val yamlContent = """
            view-one:
              prompt:
                id: test/one
                category: Test
                template: "One: {{{input}}}"
              userControls:
                modelParameters: true
            view-two:
              prompt:
                id: test/two 
                category: Test
                template: "Two: {{{input}}}"
              userControls:
                multipleResponses: true
        """.trimIndent()
        
        val configMap = MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(yamlContent)
        
        assertEquals(2, configMap.size)
        assertTrue(configMap.containsKey("view-one"))
        assertTrue(configMap.containsKey("view-two"))
        
        val viewOne = configMap["view-one"]!!
        assertEquals("test/one", viewOne.promptDef.id)
        assertEquals(true, viewOne.userControls.modelParameters)
        assertEquals(false, viewOne.userControls.multipleResponses)
        
        val viewTwo = configMap["view-two"]!!
        assertEquals("test/two", viewTwo.promptDef.id)
        assertEquals(false, viewTwo.userControls.modelParameters)
        assertEquals(true, viewTwo.userControls.multipleResponses)
    }

    @Test
    fun `test all RuntimeArgDisplayType values in YAML`() {
        val yamlContent = """
            multi-control-view:
              prompt:
                id: test/multi-control
                category: Test
                template: "Process {{mode}} and {{format}} with {{{input}}} and {{{custom}}}"
              args:
                - fieldId: mode
                  label: Mode
                  control: COMBO_BOX
                  values: ["A", "B"]
                - fieldId: input
                  label: Input Text
                  control: TEXT_AREA
                - fieldId: custom
                  label: Custom Field
                  control: TEXT_AREA
                - fieldId: format
                  label: Hidden Format
                  control: HIDDEN
                  values: ["JSON"]
        """.trimIndent()
        
        val configMap = MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(yamlContent)
        val config = configMap["multi-control-view"]!!
        
        assertEquals(4, config.args.size)
        
        val modeArg = config.args.find { it.fieldId == "mode" }!!
        assertEquals(RuntimeArgDisplayType.COMBO_BOX, modeArg.control)
        
        val inputArg = config.args.find { it.fieldId == "input" }!!
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, inputArg.control)
        
        val customArg = config.args.find { it.fieldId == "custom" }!!
        assertEquals(RuntimeArgDisplayType.TEXT_AREA, customArg.control)
        
        val formatArg = config.args.find { it.fieldId == "format" }!!
        assertEquals(RuntimeArgDisplayType.HIDDEN, formatArg.control)
        assertEquals(listOf("JSON"), formatArg.values)
    }

    @Test 
    fun `test round-trip YAML serialization`() {
        val originalConfig = RuntimePromptViewConfig(
            promptDef = tri.ai.prompt.PromptDef(
                id = "test/roundtrip",
                category = "Test",
                name = "Round Trip Test",
                template = "Test {{mode}} with {{{input}}}"
            ),
            args = listOf(
                RuntimeArgConfig(
                    fieldId = "mode",
                    label = "Mode",
                    values = listOf("A", "B")
                )
            ),
            userControls = RuntimeUserControls(
                modelParameters = true,
                multipleResponses = false
            ),
            requestJson = true
        )
        
        // Create a map to simulate YAML structure
        val configMap = mapOf("test-config" to originalConfig)
        
        // Serialize to YAML
        val yaml = MAPPER.writeValueAsString(configMap)
        assertNotNull(yaml)
        
        // Deserialize back
        val deserializedMap = MAPPER.readValue<Map<String, RuntimePromptViewConfig>>(yaml)
        val deserializedConfig = deserializedMap["test-config"]!!
        
        // Verify key properties
        assertEquals(originalConfig.promptDef.id, deserializedConfig.promptDef.id)
        assertEquals(originalConfig.args.size, deserializedConfig.args.size)
        assertEquals(originalConfig.userControls.modelParameters, deserializedConfig.userControls.modelParameters)
        assertEquals(originalConfig.requestJson, deserializedConfig.requestJson)
    }
}