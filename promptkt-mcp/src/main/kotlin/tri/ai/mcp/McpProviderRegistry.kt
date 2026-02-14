/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.McpToolResponse
import tri.ai.mcp.tool.McpToolLibraryStarter
import tri.ai.mcp.tool.McpToolLibrary
import tri.ai.prompt.PromptLibrary
import java.io.File

/**
 * Registry for MCP providers that can be configured via JSON or YAML.
 * Provides a central place to define and access multiple MCP provider configurations.
 */
class McpProviderRegistry(
    private val providers: Map<String, McpProviderConfig>
) {

    /**
     * Get a provider by name/ID.
     * @param name The name/ID of the provider
     * @return The provider, or null if not found
     */
    fun getProvider(name: String): McpProvider? {
        val config = providers[name] ?: return null
        return createProvider(config)
    }

    /**
     * List all available provider names/IDs.
     */
    fun listProviderNames(): List<String> = providers.keys.toList()

    /**
     * Get all provider configurations.
     */
    fun getConfigs(): Map<String, McpProviderConfig> = providers

    private fun createProvider(config: McpProviderConfig): McpProvider {
        return when (config) {
            is EmbeddedProviderConfig -> createEmbeddedProvider(config)
            is StdioProviderConfig -> createStdioProvider(config)
            is HttpProviderConfig -> createHttpProvider(config)
            is TestProviderConfig -> createTestProvider(config)
        }
    }

    private fun createEmbeddedProvider(config: EmbeddedProviderConfig): McpProvider {
        val prompts = if (config.promptLibraryPath != null) {
            PromptLibrary.loadFromPath(config.promptLibraryPath)
        } else {
            PromptLibrary()
        }
        return McpProviderEmbedded(prompts, McpToolLibraryStarter())
    }

    private fun createStdioProvider(config: StdioProviderConfig): McpProvider {
        return McpProviderStdio(config.command, config.args, config.env)
    }

    private fun createHttpProvider(config: HttpProviderConfig) =
        McpProviderHttp(config.url)

    private fun createTestProvider(config: TestProviderConfig): McpProvider {
        // Test provider uses a fixed set of samples/built-ins
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

        val tools: McpToolLibrary = if (config.includeDefaultTools) {
            McpToolLibraryStarter()
        } else {
            object : McpToolLibrary {
                override suspend fun listTools() = emptyList<McpToolMetadata>()
                override suspend fun getTool(name: String) = null
                override suspend fun callTool(name: String, args: Map<String, Any?>) =
                    McpToolResponse.error("Tool library is disabled")
            }
        }

        val resources = if (config.includeDefaultResources) {
            listOf(
                McpResource(
                    uri = "file:///sample-data.txt",
                    name = "Sample Text File",
                    description = "A sample text file for testing the MCP Resources view",
                    mimeType = "text/plain"
                ),
                McpResource(
                    uri = "file:///config.json",
                    name = "Configuration File",
                    description = "Sample JSON configuration for demonstration purposes",
                    mimeType = "application/json"
                ),
                McpResource(
                    uri = "data://test/example",
                    name = "Test Data Resource",
                    description = "Example data resource with custom URI scheme",
                    mimeType = "text/plain"
                )
            )
        } else {
            emptyList()
        }

        return McpProviderEmbedded(prompts, tools, resources)
    }

    companion object {
        private val configModule = SimpleModule().apply {
            addDeserializer(McpProviderConfig::class.java, McpProviderConfigDeserializer())
        }
        
        private val jsonMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(configModule)

        private val yamlMapper = ObjectMapper(YAMLFactory())
            .registerModule(KotlinModule.Builder().build())
            .registerModule(configModule)

        /**
         * Load registry from a JSON file.
         */
        fun loadFromJson(file: File): McpProviderRegistry {
            val config = jsonMapper.readValue<McpProviderRegistryConfig>(file)
            return McpProviderRegistry(config.allServers())
        }

        /**
         * Load registry from a YAML file.
         */
        fun loadFromYaml(file: File): McpProviderRegistry {
            val config = yamlMapper.readValue<McpProviderRegistryConfig>(file)
            return McpProviderRegistry(config.allServers())
        }

        /**
         * Load registry from a file (auto-detect JSON or YAML based on extension).
         */
        fun loadFromFile(path: String): McpProviderRegistry {
            val file = File(path)
            return when {
                path.endsWith(".json") -> loadFromJson(file)
                path.endsWith(".yaml") || path.endsWith(".yml") -> loadFromYaml(file)
                else -> throw IllegalArgumentException("Unsupported file format: $path. Use .json, .yaml, or .yml")
            }
        }

        /**
         * Create a default registry with built-in provider configurations.
         */
        fun default(): McpProviderRegistry {
            return McpProviderRegistry(
                mapOf(
                    "embedded" to EmbeddedProviderConfig(
                        description = "Embedded MCP provider with default libraries"
                    ),
                    "test" to TestProviderConfig(
                        description = "Test provider with sample prompts and tools"
                    )
                )
            )
        }
    }
}

/**
 * Configuration for the MCP provider registry.
 * Supports both "servers" (PromptFx format) and "mcpServers" (Claude Desktop/OpenAI format).
 */
data class McpProviderRegistryConfig(
    val servers: Map<String, McpProviderConfig>? = null,
    val mcpServers: Map<String, McpProviderConfig>? = null
) {
    /**
     * Get the server configurations.
     * Note: mcpServers takes precedence over servers when both are present,
     * allowing users to migrate from PromptFx format to Claude Desktop format.
     */
    fun allServers(): Map<String, McpProviderConfig> {
        return mcpServers ?: servers ?: emptyMap()
    }
}

/**
 * Base class for MCP provider configurations.
 * Supports both explicit type field (PromptFx format) and inferred type (Claude Desktop/OpenAI format).
 */
sealed class McpProviderConfig {
    abstract val description: String?
}

/**
 * Configuration for an embedded MCP provider (running in the same process).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class EmbeddedProviderConfig(
    override val description: String? = null,
    val promptLibraryPath: String? = null
) : McpProviderConfig()

/**
 * Configuration for a remote MCP server via stdio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StdioProviderConfig(
    override val description: String? = null,
    val command: String,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap()
) : McpProviderConfig()

/**
 * Configuration for a remote MCP server via HTTP.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HttpProviderConfig(
    override val description: String? = null,
    val url: String
) : McpProviderConfig()

/**
 * Configuration for a test provider with built-in samples.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TestProviderConfig(
    override val description: String? = null,
    val includeDefaultPrompts: Boolean = true,
    val includeDefaultTools: Boolean = true,
    val includeDefaultResources: Boolean = true
) : McpProviderConfig()

/**
 * Custom deserializer for McpProviderConfig that supports both explicit type field
 * (PromptFx format) and inferred type (Claude Desktop/OpenAI format).
 */
class McpProviderConfigDeserializer : JsonDeserializer<McpProviderConfig>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): McpProviderConfig {
        val node = p.codec.readTree<JsonNode>(p)
        val mapper = p.codec as ObjectMapper
        
        // Check if explicit type field exists (PromptFx format)
        val typeNode = node.get("type")
        if (typeNode != null) {
            return when (val type = typeNode.asText()) {
                "embedded" -> mapper.treeToValue(node, EmbeddedProviderConfig::class.java)
                "stdio" -> mapper.treeToValue(node, StdioProviderConfig::class.java)
                "http" -> mapper.treeToValue(node, HttpProviderConfig::class.java)
                "test" -> mapper.treeToValue(node, TestProviderConfig::class.java)
                else -> throw IllegalArgumentException("Unknown provider type: \"$type\". Valid types are: embedded, stdio, http, test")
            }
        }
        
        // Infer type from fields (Claude Desktop/OpenAI format)
        return when {
            node.has("command") -> mapper.treeToValue(node, StdioProviderConfig::class.java)
            node.has("url") -> mapper.treeToValue(node, HttpProviderConfig::class.java)
            else -> throw IllegalArgumentException("Cannot infer provider type: expected either \"command\" field for stdio server or \"url\" field for HTTP server, but neither was found in config: $node")
        }
    }
}
