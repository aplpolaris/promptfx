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
package tri.promptfx.tools

import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.PromptFxModels

class GlobalConfigurationDataTest {
    @Test
    fun testConfigurationDataAccess() {
        println("Testing configuration data access...")
        
        // Test policy access
        val policy = PromptFxModels.policy
        println("Policy: $policy")
        println("Show Banner: ${policy.isShowBanner}")
        println("Show Usage: ${policy.isShowUsage}")
        println("Show API Key Button: ${policy.isShowApiKeyButton}")
        
        // Test model access
        val textModels = PromptFxModels.textCompletionModels()
        println("Text Completion Models: ${textModels.size}")
        textModels.forEach { println("  - ${it.modelId}") }
        
        val chatModels = PromptFxModels.chatModels()
        println("Chat Models: ${chatModels.size}")
        chatModels.forEach { println("  - ${it.modelId}") }
        
        val embeddingModels = PromptFxModels.embeddingModels()
        println("Embedding Models: ${embeddingModels.size}")
        embeddingModels.forEach { println("  - ${it.modelId}") }
        
        // Test plugin access
        val plugins = TextPlugin.orderedPlugins
        println("Plugins: ${plugins.size}")
        plugins.forEach { plugin ->
            println("  - ${plugin.modelSource()}")
            println("    Models: ${plugin.modelInfo().size}")
            println("    Text Completion: ${plugin.textCompletionModels().size}")
            println("    Chat: ${plugin.chatModels().size}")
            println("    Embedding: ${plugin.embeddingModels().size}")
        }
        
        // Test prompt library access
        println("Built-in Prompts: ${AiPromptLibrary.INSTANCE.prompts.size}")
        println("Runtime Prompts: ${AiPromptLibrary.RUNTIME_INSTANCE.prompts.size}")
        
        println("Configuration data access test completed successfully.")
    }
}