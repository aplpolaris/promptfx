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
package tri.util.ui.starship

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.core.tool.impl.PromptChatRegistry
import tri.ai.openai.OpenAiPlugin
import tri.ai.pips.PrintMonitor
import tri.ai.pips.api.PPlanExecutor
import tri.ai.prompt.PromptLibrary
import java.io.File

class StarshipConfigTest {

    @Test
    fun testLoad() {
        val config = StarshipConfig.readDefaultYaml()
        println("Loaded plan: ${config.pipeline}")
    }

    @Test
    fun testReadConfig() {
        // Test that readConfig() works when no custom file exists
        val config = StarshipConfig.readConfig()
        println("Loaded config with pipeline: ${config.pipeline.id}")
        assertTrue(config.pipeline.steps.isNotEmpty())
    }

    @Test
    fun testReadConfigWithFile() {
        // Save reference to any existing file
        val configFile = File("starship.yaml")
        val existingContent = if (configFile.exists()) configFile.readText() else null
        
        val customContent = """
            question:
              template: "Custom question template"
              topics: ["custom topic"]
              examples: ["custom example"]
              lists: {}
            pipeline:
              id: custom/test@1.0.0
              steps: []
            layout:
              backgroundIcon: STAR
              backgroundIconCount: 500
              numCols: 2
              numRows: 2
              isShowGrid: false
              widgets: []
        """.trimIndent()
        
        try {
            // Create config file in root directory
            configFile.writeText(customContent)
            
            // Load config - should pick up the config file
            val config = StarshipConfig.readConfig()
            
            // Verify config was loaded
            assertEquals("custom/test@1.0.0", config.pipeline.id)
            assertEquals("Custom question template", config.question.template)
            assertEquals(500, config.layout.backgroundIconCount)
            
            println("Successfully loaded config: ${config.pipeline.id}")
        } finally {
            // Restore original state - only clean up the file we created
            if (existingContent != null) {
                configFile.writeText(existingContent)
            } else if (configFile.exists()) {
                configFile.delete()
            }
        }
    }

    @Test
    fun testRandomQuestion() {
        val config = StarshipConfig.readDefaultYaml()
        val questioner = StarshipExecutableQuestionGenerator(config.question, OpenAiPlugin().chatModels().first())
        println(questioner.randomQuestion())
        println(questioner.randomQuestion())
    }

    @Disabled("Need alternative to the current view executor")
    @Test
    fun testExecute() {
        val config = StarshipConfig.readDefaultYaml()
        val chat = OpenAiPlugin().chatModels().first()
        val registry = ExecutableRegistry.Companion.create(
            listOf(StarshipExecutableQuestionGenerator(config.question, chat)) +
                    PromptChatRegistry(PromptLibrary.Companion.INSTANCE, chat).list()
        )

        runBlocking {
            PPlanExecutor(registry).execute(config.pipeline, ExecContext(), PrintMonitor())
        }
    }
}
