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
package tri.ai.anthropic

import org.junit.jupiter.api.Test
import tri.ai.core.TextPlugin

class AnthropicModelIndexTest {

    @Test
    fun testModels() {
        println("Chat models: ${AnthropicModelIndex.chatModels()}")
        println("Multimodal models: ${AnthropicModelIndex.multimodalModels()}")
        println("Vision language models: ${AnthropicModelIndex.visionLanguageModels()}")
    }

    @Test
    fun testPluginRegistration() {
        val anthropicPlugins = TextPlugin.orderedPlugins.filterIsInstance<AnthropicPlugin>()
        println("Found ${anthropicPlugins.size} Anthropic plugin(s)")
        anthropicPlugins.forEach { plugin ->
            println("Plugin source: ${plugin.modelSource()}")
            println("Available models: ${plugin.modelInfo().map { "${it.id} (${it.type})" }}")
        }
    }

}