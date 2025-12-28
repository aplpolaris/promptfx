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
package tri.promptfx

import tornadofx.Controller
import tri.ai.mcp.McpProviderRegistry
import java.io.File

/** Controller for MCP servers configuration in [PromptFx]. */
class PromptFxMcpController : Controller() {

    /** Registry of MCP servers loaded from configuration file. */
    val mcpProviderRegistry: McpProviderRegistry = loadRegistry()

    private fun loadRegistry(): McpProviderRegistry {
        // First, try to load from runtime config files
        val runtimeFile = setOf(
            File("mcp-servers.yaml"),
            File("config/mcp-servers.yaml")
        ).firstOrNull { it.exists() }
        
        if (runtimeFile != null) {
            return McpProviderRegistry.loadFromYaml(runtimeFile)
        }
        
        // Fall back to built-in resource file
        val configPath = "tri/promptfx/resources/mcp-servers.yaml"
        val configStream = javaClass.classLoader.getResourceAsStream(configPath)
        
        return if (configStream != null) {
            val tempFile = File.createTempFile("mcp-servers", ".yaml")
            tempFile.deleteOnExit()
            tempFile.outputStream().use { output ->
                configStream.use { input ->
                    input.copyTo(output)
                }
            }
            McpProviderRegistry.loadFromYaml(tempFile)
        } else {
            // Fallback to default registry if no config file found
            McpProviderRegistry.default()
        }
    }
}
