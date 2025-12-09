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
        assertTrue(serverNames.contains("embedded"))
        assertTrue(serverNames.contains("test"))
    }

    @Test
    fun testGetEmbeddedServer() {
        runTest {
            val registry = McpServerRegistry.default()
            val server = registry.getServer("embedded")
            
            assertNotNull(server)
            assertTrue(server is McpServerEmbedded)
            
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
            assertTrue(server is McpServerEmbedded)
            
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
              test-embedded:
                type: embedded
                description: "Test embedded server"
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
        assertTrue(serverNames.contains("test-embedded"))
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
                "test-embedded": {
                  "type": "embedded",
                  "description": "Test embedded server"
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
        assertTrue(serverNames.contains("test-embedded"))
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
        assertTrue(configs.containsKey("embedded"))
        assertTrue(configs.containsKey("test"))
        
        val embeddedConfig = configs["embedded"]
        assertTrue(embeddedConfig is EmbeddedServerConfig)
        
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
            assertTrue(server is McpServerAdapterHttp)
            
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
                type: embedded
                description: "Embedded server with custom prompts"
                promptLibraryPath: "${promptFile.toString().replace("\\", "\\\\")}"
        """.trimIndent()
        )

        runTest {
            val registry = McpServerRegistry.loadFromFile(yamlFile.toString())
            val server = registry.getServer("custom")
            
            assertNotNull(server)
            assertTrue(server is McpServerEmbedded)
            
            val prompts = server!!.listPrompts()
            assertTrue(prompts.any { it.name == "test/custom-prompt" })
            
            server.close()
        }
    }

    @Test
    fun testTestServerConfigFlags() {
        runTest {
            // Test with prompts only
            val yamlFile1 = tempDir.resolve("test-prompts-only.yaml")
            Files.writeString(
                yamlFile1, """
                servers:
                  test-prompts-only:
                    type: test
                    description: "Test server with prompts only"
                    includeDefaultPrompts: true
                    includeDefaultTools: false
            """.trimIndent()
            )

            val registry1 = McpServerRegistry.loadFromFile(yamlFile1.toString())
            val server1 = registry1.getServer("test-prompts-only")!!
            
            val prompts1 = server1.listPrompts()
            val tools1 = server1.listTools()
            
            assertTrue(prompts1.isNotEmpty(), "Should have prompts")
            assertTrue(tools1.isEmpty(), "Should not have tools")
            
            server1.close()

            // Test with tools only
            val yamlFile2 = tempDir.resolve("test-tools-only.yaml")
            Files.writeString(
                yamlFile2, """
                servers:
                  test-tools-only:
                    type: test
                    description: "Test server with tools only"
                    includeDefaultPrompts: false
                    includeDefaultTools: true
            """.trimIndent()
            )

            val registry2 = McpServerRegistry.loadFromFile(yamlFile2.toString())
            val server2 = registry2.getServer("test-tools-only")!!
            
            val prompts2 = server2.listPrompts()
            val tools2 = server2.listTools()
            
            assertTrue(prompts2.isEmpty(), "Should not have prompts")
            assertTrue(tools2.isNotEmpty(), "Should have tools")
            
            server2.close()

            // Test with neither
            val yamlFile3 = tempDir.resolve("test-empty.yaml")
            Files.writeString(
                yamlFile3, """
                servers:
                  test-empty:
                    type: test
                    description: "Empty test server"
                    includeDefaultPrompts: false
                    includeDefaultTools: false
            """.trimIndent()
            )

            val registry3 = McpServerRegistry.loadFromFile(yamlFile3.toString())
            val server3 = registry3.getServer("test-empty")!!
            
            val prompts3 = server3.listPrompts()
            val tools3 = server3.listTools()
            
            assertTrue(prompts3.isEmpty(), "Should not have prompts")
            assertTrue(tools3.isEmpty(), "Should not have tools")
            
            server3.close()
        }
    }
}
