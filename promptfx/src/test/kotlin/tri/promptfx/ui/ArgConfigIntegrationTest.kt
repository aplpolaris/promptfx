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
import tri.promptfx.RuntimePromptViewConfigs

class ArgConfigIntegrationTest {

    @Test
    fun testStructuredDataExtractionViewConfig() {
        // Test that the structured data extraction view loads properly
        val viewIndex = RuntimePromptViewConfigs.viewIndex
        val structuredDataView = viewIndex["Structured Data Extraction"]
        
        assertNotNull(structuredDataView, "Structured Data Extraction view should be loaded")
        assertNotNull(structuredDataView?.config, "View config should not be null")
        
        val config = structuredDataView!!.config!!
        assertEquals("text-extract/text-to-json", config.promptDef.id)
        assertTrue(config.requestJson, "Should request JSON output")
        assertTrue(config.isShowModelParameters, "Should show model parameters")
        
        // Check argOptions
        assertTrue(config.argOptions.isNotEmpty(), "Should have argument options")
        
        val argsByTemplateId = config.argOptions.associateBy { it.templateId }
        
        // Input text area
        val inputArg = argsByTemplateId["input"]
        assertNotNull(inputArg, "Should have input argument")
        assertEquals(ArgDisplayType.TEXT_AREA, inputArg?.displayType)
        assertEquals("Source Text", inputArg?.label)
        
        // Example text area
        val exampleArg = argsByTemplateId["example"]
        assertNotNull(exampleArg, "Should have example argument")
        assertEquals(ArgDisplayType.TEXT_AREA, exampleArg?.displayType)
        assertEquals("Sample Output", exampleArg?.label)
        
        // Combo box args
        val guidanceArg = argsByTemplateId["guidance"]
        assertNotNull(guidanceArg, "Should have guidance argument")
        assertEquals(ArgDisplayType.COMBO_BOX, guidanceArg?.displayType)
        assertNotNull(guidanceArg?.values, "Guidance should have predefined values")
        
        val formatArg = argsByTemplateId["format"]
        assertNotNull(formatArg, "Should have format argument")
        assertEquals(ArgDisplayType.COMBO_BOX, formatArg?.displayType)
        assertEquals("structured-format", formatArg?.id)
    }

    @Test
    fun testQuestionAnsweringViewConfig() {
        // Test that the question answering view loads properly
        val viewIndex = RuntimePromptViewConfigs.viewIndex
        val qaView = viewIndex["Question Answering"]
        
        assertNotNull(qaView, "Question Answering view should be loaded")
        assertNotNull(qaView?.config, "View config should not be null")
        
        val config = qaView!!.config!!
        assertEquals("docs-qa/answer", config.promptDef.id)
        assertTrue(config.isShowModelParameters, "Should show model parameters")
        
        // Check argOptions
        assertTrue(config.argOptions.isNotEmpty(), "Should have argument options")
        
        val argsByTemplateId = config.argOptions.associateBy { it.templateId }
        
        // Question text area
        val questionArg = argsByTemplateId["question"]
        assertNotNull(questionArg, "Should have question argument")
        assertEquals(ArgDisplayType.TEXT_AREA, questionArg?.displayType)
        assertEquals("Question", questionArg?.label)
        
        // Context text area
        val contextArg = argsByTemplateId["context"]
        assertNotNull(contextArg, "Should have context argument")
        assertEquals(ArgDisplayType.TEXT_AREA, contextArg?.displayType)
        assertEquals("Context", contextArg?.label)
        
        // Answer style combo box
        val styleArg = argsByTemplateId["answer_style"]
        assertNotNull(styleArg, "Should have answer_style argument")
        assertEquals(ArgDisplayType.COMBO_BOX, styleArg?.displayType)
        assertEquals("detailed", styleArg?.defaultValue)
        
        // Hidden sources argument
        val sourcesArg = argsByTemplateId["include_sources"]
        assertNotNull(sourcesArg, "Should have include_sources argument")
        assertEquals(ArgDisplayType.HIDDEN, sourcesArg?.displayType)
        assertEquals("yes", sourcesArg?.defaultValue)
    }

    @Test
    fun testBackwardCompatibilityStillWorks() {
        // Test that existing views with modeOptions still work
        val viewIndex = RuntimePromptViewConfigs.viewIndex
        val sentimentView = viewIndex["Classify Sentiment"]
        
        assertNotNull(sentimentView, "Sentiment view should still be loaded")
        val config = sentimentView!!.config!!
        
        // This view should use the old modeOptions approach
        assertTrue(config.argOptions.isEmpty(), "New argOptions should be empty for legacy configs")
        assertTrue(config.modeOptions.isNotEmpty(), "Should have legacy modeOptions")
        assertTrue(config.allArgOptions.isNotEmpty(), "Combined options should include legacy modes")
        
        // All legacy modes should default to COMBO_BOX display type
        config.allArgOptions.forEach { arg ->
            assertEquals(ArgDisplayType.COMBO_BOX, arg.displayType)
        }
    }
}