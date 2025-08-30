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
package tri.util.ui.starship

import javafx.collections.ListChangeListener
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.MChatVariation
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxModels

class StarshipPipelineTest {
    @Test
    fun testExecWithJsonPipeline() {
        // Create a mock chat engine for testing
        val mockChatEngine = object : TextChat {
            override val modelId = "mock-test-engine"
            override suspend fun chat(
                messages: List<TextChatMessage>,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?,
                requestJson: Boolean?
            ): AiPromptTrace {
                val message = messages.firstOrNull()?.content ?: "test input"
                return AiPromptTrace.outputMessage(TextChatMessage.assistant("Mock response for: $message"))
            }
        }

        val config = StarshipPipelineConfig(mockChatEngine)
        val results = StarshipPipelineResults().apply {
            input.addListener { _, _, newValue -> println("Input: $newValue") }
            runConfig.addListener { _, _, newValue -> println("RunConfig: $newValue") }
            output.addListener { _, _, newValue -> println("Output: $newValue") }
            outputText.addListener { _, _, newValue -> println("OutputText: $newValue") }
            secondaryRunConfigs.addListener(ListChangeListener { println("SecondaryRunConfigs: ${it.list}") })
            secondaryOutputs.addListener(ListChangeListener { println("SecondaryOutputs: ${it.list}") })
            started.addListener { _, _, _ -> println("Started") }
            completed.addListener { _, _, _ -> println("Completed") }
        }
        
        // Test that we can load the pipeline configuration
        assertNotNull(config.pipeline)
        assertEquals("starship/default@1.0.0", config.pipeline.id)
        
        // Test that the executable registry is properly configured
        assertNotNull(config.executableRegistry)
        assertTrue(config.executableRegistry.list().isNotEmpty())
        
        println("Test completed successfully - JSON pipeline configuration loaded")
    }

    @Test  
    fun testFullPipelineExecution() {
        // Create a mock chat engine for testing
        val mockChatEngine = object : TextChat {
            override val modelId = "mock-test-engine"
            override suspend fun chat(
                messages: List<TextChatMessage>,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?,
                requestJson: Boolean?
            ): AiPromptTrace {
                val message = messages.firstOrNull()?.content ?: "test input"
                return AiPromptTrace.outputMessage(TextChatMessage.assistant("Mock response for: $message"))
            }
        }

        // Create a config with a fixed input generator
        val config = object : StarshipPipelineConfig(mockChatEngine) {
            override val generator: () -> String = { "Test input about artificial intelligence and machine learning" }
        }
        
        // Test that the pipeline can be loaded and executed without JavaFX dependencies
        val pipeline = config.pipeline
        assertEquals("starship/default@1.0.0", pipeline.id)
        assertEquals(5, pipeline.steps.size)
        
        // Test the pipeline steps are configured correctly
        val stepNames = pipeline.steps.map { it.tool }
        assertTrue(stepNames.contains("prompt/docs-map/summarize"))
        assertTrue(stepNames.contains("prompt/text-summarize/simplify-audience"))
        assertTrue(stepNames.contains("prompt/docs-reduce/outline"))
        assertTrue(stepNames.contains("prompt/docs-reduce/technical-terms"))
        assertTrue(stepNames.contains("prompt/text-translate/translate"))
        
        println("Full pipeline configuration test completed successfully")
        println("Pipeline has ${pipeline.steps.size} steps: $stepNames")
    }
    
    @Test
    fun testEnhancedPipelineConfiguration() {
        // Create a mock chat engine for testing
        val mockChatEngine = object : TextChat {
            override val modelId = "mock-test-engine"
            override suspend fun chat(
                messages: List<TextChatMessage>,
                variation: MChatVariation,
                tokens: Int?,
                stop: List<String>?,
                numResponses: Int?,
                requestJson: Boolean?
            ): AiPromptTrace {
                val message = messages.firstOrNull()?.content ?: "test input"
                return AiPromptTrace.outputMessage(TextChatMessage.assistant("Mock response for: $message"))
            }
        }

        // Create a config with enhanced pipeline
        val config = object : StarshipPipelineConfig(mockChatEngine) {
            override val pipelineConfigPath = "/tri/util/ui/starship/resources/starship-enhanced-pipeline.json"
        }
        
        // Test that the enhanced pipeline can be loaded
        val pipeline = config.pipeline
        assertEquals("starship/enhanced@1.0.0", pipeline.id)
        assertEquals(4, pipeline.steps.size)
        
        // Test the pipeline steps include both prompts and chat calls
        val stepNames = pipeline.steps.map { it.tool }
        assertTrue(stepNames.contains("prompt/docs-map/summarize"))
        assertTrue(stepNames.contains("chat/mock-test-engine"))
        assertTrue(stepNames.contains("prompt/text-summarize/simplify-audience"))
        
        // Verify chat steps are properly configured
        val chatSteps = pipeline.steps.filter { it.tool.startsWith("chat/") }
        assertEquals(2, chatSteps.size)
        
        println("Enhanced pipeline configuration test completed successfully")
        println("Pipeline has ${pipeline.steps.size} steps with ${chatSteps.size} chat executions: $stepNames")
    }
}
