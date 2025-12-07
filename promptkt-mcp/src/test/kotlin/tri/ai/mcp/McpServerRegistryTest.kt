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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class McpServerRegistryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testDefaultRegistry() {
        val registry = McpServerRegistry.default()
        
        val serverNames = registry.listServerNames()
        assertTrue(serverNames.contains("local"))
        assertTrue(serverNames.contains("test"))
    }

    @Test
    fun testGetLocalServer() {
        runTest {
            val registry = McpServerRegistry.default()
            val server = registry.getServer("local")
            
            assertNotNull(server)
            assertTrue(server is LocalMcpServer)
            
            // Verify it works
            val prompts = server!!.listPrompts()
            assertNotNull(prompts)
            
            server.close()
        }
    }

    @Test
    fun testGetTestServer() {
        runTest {
            val registry = McpServerRegistry.default()
            val server = registry.getServer("test")
            
            assertNotNull(server)
            assertTrue(server is LocalMcpServer)
            
            // Verify it has prompts and tools
            val prompts = server!!.listPrompts()
            assertNotNull(prompts)
            assertTrue(prompts.isNotEmpty())
            
            val tools = server.listTools()
            assertNotNull(tools)
            assertTrue(tools.isNotEmpty())
            
            server.close()
        }
    }

    @Test
    fun testGetNonExistentServer() {
        val registry = McpServerRegistry.default()
        val server = registry.getServer("nonexistent")
        
        assertNull(server)
    }

    @Test
    fun testLoadFromYaml() {
        val yamlFile = tempDir.resolve("test-registry.yaml")
        Files.writeString(
            yamlFile, """
            servers:
              test-local:
                type: local
                description: "Test local server"
              test-http:
                type: http
                description: "Test HTTP server"
                url: "http://localhost:8080/mcp"
              test-test:
                type: test
                description: "Test test server"
        """.trimIndent()
        )

        val registry = McpServerRegistry.loadFromYaml(yamlFile.toFile())
        
        val serverNames = registry.listServerNames()
        assertEquals(3, serverNames.size)
        assertTrue(serverNames.contains("test-local"))
        assertTrue(serverNames.contains("test-http"))
        assertTrue(serverNames.contains("test-test"))
    }

    @Test
    fun testLoadFromJson() {
        val jsonFile = tempDir.resolve("test-registry.json")
        Files.writeString(
            jsonFile, """
            {
              "servers": {
                "test-local": {
                  "type": "local",
                  "description": "Test local server"
                },
                "test-http": {
                  "type": "http",
                  "description": "Test HTTP server",
                  "url": "http://localhost:8080/mcp"
                }
              }
            }
        """.trimIndent()
        )

        val registry = McpServerRegistry.loadFromJson(jsonFile.toFile())
        
        val serverNames = registry.listServerNames()
        assertEquals(2, serverNames.size)
        assertTrue(serverNames.contains("test-local"))
        assertTrue(serverNames.contains("test-http"))
    }

    @Test
    fun testLoadFromFile_Yaml() {
        val yamlFile = tempDir.resolve("test-registry.yaml")
        Files.writeString(
            yamlFile, """
            servers:
              test-server:
                type: test
                description: "Test server"
        """.trimIndent()
        )

        val registry = McpServerRegistry.loadFromFile(yamlFile.toString())
        
        val serverNames = registry.listServerNames()
        assertEquals(1, serverNames.size)
        assertTrue(serverNames.contains("test-server"))
    }

    @Test
    fun testLoadFromFile_Json() {
        val jsonFile = tempDir.resolve("test-registry.json")
        Files.writeString(
            jsonFile, """
            {
              "servers": {
                "test-server": {
                  "type": "test",
                  "description": "Test server"
                }
              }
            }
        """.trimIndent()
        )

        val registry = McpServerRegistry.loadFromFile(jsonFile.toString())
        
        val serverNames = registry.listServerNames()
        assertEquals(1, serverNames.size)
        assertTrue(serverNames.contains("test-server"))
    }

    @Test
    fun testLoadFromFile_UnsupportedFormat() {
        val txtFile = tempDir.resolve("test-registry.txt")
        Files.writeString(txtFile, "invalid content")

        assertThrows(IllegalArgumentException::class.java) {
            McpServerRegistry.loadFromFile(txtFile.toString())
        }
    }

    @Test
    fun testGetConfigs() {
        val registry = McpServerRegistry.default()
        val configs = registry.getConfigs()
        
        assertNotNull(configs)
        assertTrue(configs.containsKey("local"))
        assertTrue(configs.containsKey("test"))
        
        val localConfig = configs["local"]
        assertTrue(localConfig is LocalServerConfig)
        
        val testConfig = configs["test"]
        assertTrue(testConfig is TestServerConfig)
    }

    @Test
    fun testHttpServerConfig() {
        runTest {
            val yamlFile = tempDir.resolve("http-registry.yaml")
            Files.writeString(
                yamlFile, """
                servers:
                  remote:
                    type: http
                    description: "Remote HTTP server"
                    url: "http://example.com/mcp"
            """.trimIndent()
            )

            val registry = McpServerRegistry.loadFromFile(yamlFile.toString())
            val server = registry.getServer("remote")
            
            assertNotNull(server)
            assertTrue(server is RemoteMcpServer)
            
            server!!.close()
        }
    }

    @Test
    fun testLocalServerWithCustomPromptLibrary() {
        val promptFile = tempDir.resolve("custom-prompts.yaml")
        Files.writeString(
            promptFile, """
            id: test-group
            title: Test Group
            description: Test prompt group
            prompts:
              - id: test/custom-prompt
                title: Custom Prompt
                description: A custom test prompt
                template: |
                  Custom prompt: {{input}}
        """.trimIndent()
        )

        val yamlFile = tempDir.resolve("registry.yaml")
        Files.writeString(
            yamlFile, """
            servers:
              custom:
                type: local
                description: "Local server with custom prompts"
                promptLibraryPath: "${promptFile.toString()}"
        """.trimIndent()
        )

        runTest {
            val registry = McpServerRegistry.loadFromFile(yamlFile.toString())
            val server = registry.getServer("custom")
            
            assertNotNull(server)
            assertTrue(server is LocalMcpServer)
            
            val prompts = server!!.listPrompts()
            assertTrue(prompts.any { it.name == "test/custom-prompt" })
            
            server.close()
        }
    }
}
