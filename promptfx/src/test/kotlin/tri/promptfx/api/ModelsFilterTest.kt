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
package tri.promptfx.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.DataModality
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType

class ModelsFilterTest {

    @Test
    fun testModalityFilterWithEmptyModalities() {
        // Create a model with no modalities (null inputs/outputs)
        val modelWithoutModalities = ModelInfo("model1", ModelType.TEXT_CHAT, "test-source")
        
        // Create a model with modalities
        val modelWithModalities = ModelInfo("model2", ModelType.TEXT_CHAT, "test-source").apply {
            inputs = listOf(DataModality.text)
            outputs = listOf(DataModality.text)
        }
        
        // Create filter with text modality selected
        val filter = ModalityFilter { it.inputs ?: emptyList() }
        filter.updateAttributeOptions(setOf(modelWithoutModalities, modelWithModalities)) {}
        
        // Get the text modality and select it
        val textModalityPair = filter.values.find { it.first == DataModality.text }
        assertNotNull(textModalityPair, "Text modality should be available in filter")
        textModalityPair!!.second.value = true
        
        val filterFunc = filter.createFilter()
        
        // Model without modalities should pass the filter (returns true)
        assertTrue(filterFunc(modelWithoutModalities), 
            "Model without modalities should be shown when filters are applied")
        
        // Model with matching modalities should pass the filter
        assertTrue(filterFunc(modelWithModalities), 
            "Model with matching modalities should be shown")
    }
    
    @Test
    fun testModalityFilterWithEmptyList() {
        // Create a model with empty modalities list
        val modelWithEmptyModalities = ModelInfo("model1", ModelType.TEXT_CHAT, "test-source").apply {
            inputs = emptyList()
            outputs = emptyList()
        }
        
        // Create a model with modalities
        val modelWithModalities = ModelInfo("model2", ModelType.TEXT_CHAT, "test-source").apply {
            inputs = listOf(DataModality.text)
            outputs = listOf(DataModality.text)
        }
        
        // Create filter with text modality selected
        val filter = ModalityFilter { it.inputs ?: emptyList() }
        filter.updateAttributeOptions(setOf(modelWithEmptyModalities, modelWithModalities)) {}
        
        // Get the text modality and select it
        val textModalityPair = filter.values.find { it.first == DataModality.text }
        assertNotNull(textModalityPair, "Text modality should be available in filter")
        textModalityPair!!.second.value = true
        
        val filterFunc = filter.createFilter()
        
        // Model with empty modalities list should pass the filter (returns true)
        assertTrue(filterFunc(modelWithEmptyModalities), 
            "Model with empty modalities list should be shown when filters are applied")
        
        // Model with matching modalities should pass the filter
        assertTrue(filterFunc(modelWithModalities), 
            "Model with matching modalities should be shown")
    }
    
    @Test
    fun testModalityFilterWithNoSelection() {
        // Create models
        val model1 = ModelInfo("model1", ModelType.TEXT_CHAT, "test-source")
        val model2 = ModelInfo("model2", ModelType.TEXT_CHAT, "test-source").apply {
            inputs = listOf(DataModality.text)
        }
        
        // Create filter with no modality selected
        val filter = ModalityFilter { it.inputs ?: emptyList() }
        filter.updateAttributeOptions(setOf(model1, model2)) {}
        filter.selectNone()
        
        val filterFunc = filter.createFilter()
        
        // All models should pass when no filters are selected
        assertTrue(filterFunc(model1), "Model should be shown when no filters are selected")
        assertTrue(filterFunc(model2), "Model should be shown when no filters are selected")
    }
    
    @Test
    fun testModalityFilterWithNonMatchingModality() {
        // Create a model with audio modality
        val modelWithAudio = ModelInfo("model1", ModelType.TEXT_CHAT, "test-source").apply {
            inputs = listOf(DataModality.audio)
        }
        
        // Create a model with text modality
        val modelWithText = ModelInfo("model2", ModelType.TEXT_CHAT, "test-source").apply {
            inputs = listOf(DataModality.text)
        }
        
        // Create filter with only text modality selected
        val filter = ModalityFilter { it.inputs ?: emptyList() }
        filter.updateAttributeOptions(setOf(modelWithAudio, modelWithText)) {}
        
        // Select only text modality
        filter.values.forEach { (modality, prop) ->
            prop.value = (modality == DataModality.text)
        }
        
        val filterFunc = filter.createFilter()
        
        // Model with non-matching modality should NOT pass the filter
        assertFalse(filterFunc(modelWithAudio), 
            "Model with non-matching modality should be filtered out")
        
        // Model with matching modality should pass the filter
        assertTrue(filterFunc(modelWithText), 
            "Model with matching modality should be shown")
    }
}
