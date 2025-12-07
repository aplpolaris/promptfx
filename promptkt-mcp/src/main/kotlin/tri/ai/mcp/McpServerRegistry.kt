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
package tri.ai.mcp

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.mcp.tool.StarterToolLibrary
import tri.ai.mcp.tool.ToolLibrary
import tri.ai.prompt.PromptLibrary
import java.io.File

/**
 * Registry for MCP servers that can be configured via JSON or YAML.
 * Provides a central place to define and access multiple MCP server configurations.
 */
class McpServerRegistry(
    private val servers: Map<String, McpServerConfig>
) {
    
    /**
     * Get a server adapter by name/ID.
     * @param name The name/ID of the server
     * @return The server adapter, or null if not found
     */
    fun getServer(name: String): McpServerAdapter? {
        val config = servers[name] ?: return null
        return createAdapter(config)
    }
    
    /**
     * List all available server names/IDs.
     */
    fun listServerNames(): List<String> = servers.keys.toList()
    
    /**
     * Get all server configurations.
     */
    fun getConfigs(): Map<String, McpServerConfig> = servers
    
    private fun createAdapter(config: McpServerConfig): McpServerAdapter {
        return when (config) {
            is LocalServerConfig -> createLocalServer(config)
            is StdioServerConfig -> createStdioServer(config)
            is HttpServerConfig -> createHttpServer(config)
            is TestServerConfig -> createTestServer(config)
        }
    }
    
    private fun createLocalServer(config: LocalServerConfig): McpServerAdapter {
        val prompts = if (config.promptLibraryPath != null) {
            PromptLibrary.loadFromPath(config.promptLibraryPath)
        } else {
            PromptLibrary()
        }
        
        val tools: ToolLibrary = StarterToolLibrary()
        
        return LocalMcpServer(prompts, tools)
    }
    
    private fun createStdioServer(config: StdioServerConfig): McpServerAdapter {
        throw UnsupportedOperationException("Stdio server connections are not yet implemented in registry")
    }
    
    private fun createHttpServer(config: HttpServerConfig): McpServerAdapter {
        return RemoteMcpServer(config.url)
    }
    
    private fun createTestServer(config: TestServerConfig): McpServerAdapter {
        // Test server uses a fixed set of samples/built-ins
        val prompts = if (config.includeDefaultPrompts) {
            PromptLibrary().apply {
                // Add sample prompts from the main library
                PromptLibrary.INSTANCE
                    .list { it.category?.startsWith("research") == true }
                    .forEach { addPrompt(it) }
            }
        } else {
            PromptLibrary()
        }
        
        val tools: ToolLibrary = if (config.includeDefaultTools) {
            StarterToolLibrary()
        } else {
            object : ToolLibrary {
                override suspend fun listTools() = emptyList<tri.ai.core.tool.Executable>()
                override suspend fun getTool(name: String) = null
                override suspend fun callTool(name: String, args: Map<String, String>) =
                    tri.ai.mcp.tool.McpToolResult.error(name, "Tool library is disabled")
            }
        }
        
        return LocalMcpServer(prompts, tools)
    }
    
    companion object {
        private val jsonMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
        
        private val yamlMapper = ObjectMapper(YAMLFactory())
            .registerModule(KotlinModule.Builder().build())
        
        /**
         * Load registry from a JSON file.
         */
        fun loadFromJson(file: File): McpServerRegistry {
            val config = jsonMapper.readValue<McpServerRegistryConfig>(file)
            return McpServerRegistry(config.servers)
        }
        
        /**
         * Load registry from a YAML file.
         */
        fun loadFromYaml(file: File): McpServerRegistry {
            val config = yamlMapper.readValue<McpServerRegistryConfig>(file)
            return McpServerRegistry(config.servers)
        }
        
        /**
         * Load registry from a file (auto-detect JSON or YAML based on extension).
         */
        fun loadFromFile(path: String): McpServerRegistry {
            val file = File(path)
            return when {
                path.endsWith(".json") -> loadFromJson(file)
                path.endsWith(".yaml") || path.endsWith(".yml") -> loadFromYaml(file)
                else -> throw IllegalArgumentException("Unsupported file format: $path. Use .json, .yaml, or .yml")
            }
        }
        
        /**
         * Create a default registry with built-in server configurations.
         */
        fun default(): McpServerRegistry {
            return McpServerRegistry(
                mapOf(
                    "local" to LocalServerConfig(
                        description = "Local MCP server with default libraries"
                    ),
                    "test" to TestServerConfig(
                        description = "Test server with sample prompts and tools"
                    )
                )
            )
        }
    }
}

/**
 * Configuration for the MCP server registry.
 */
data class McpServerRegistryConfig(
    val servers: Map<String, McpServerConfig>
)

/**
 * Base class for MCP server configurations.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = LocalServerConfig::class, name = "local"),
    JsonSubTypes.Type(value = StdioServerConfig::class, name = "stdio"),
    JsonSubTypes.Type(value = HttpServerConfig::class, name = "http"),
    JsonSubTypes.Type(value = TestServerConfig::class, name = "test")
)
sealed class McpServerConfig {
    abstract val description: String?
}

/**
 * Configuration for a local MCP server (running in the same process).
 */
data class LocalServerConfig(
    override val description: String? = null,
    val promptLibraryPath: String? = null
) : McpServerConfig()

/**
 * Configuration for a remote MCP server via stdio.
 */
data class StdioServerConfig(
    override val description: String? = null,
    val command: String,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap()
) : McpServerConfig()

/**
 * Configuration for a remote MCP server via HTTP.
 */
data class HttpServerConfig(
    override val description: String? = null,
    val url: String
) : McpServerConfig()

/**
 * Configuration for a test server with built-in samples.
 */
data class TestServerConfig(
    override val description: String? = null,
    val includeDefaultPrompts: Boolean = true,
    val includeDefaultTools: Boolean = true
) : McpServerConfig()
