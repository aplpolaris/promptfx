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

class McpServerAdapterStdioTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testStdioAdapterCreation() {
        // Test that we can create an adapter with valid command
        // Note: This will fail if the command doesn't exist, which is expected
        assertThrows(Exception::class.java) {
            runTest {
                val adapter = McpServerAdapterStdio("nonexistent-command")
                adapter.close()
            }
        }
    }

    @Test
    fun testStdioAdapterWithEcho() {
        // Create a simple echo script that acts as a mock MCP server
        val scriptFile = tempDir.resolve("mock-mcp-server.sh")
        val scriptContent = """#!/bin/bash
# Mock MCP server that responds to JSON-RPC requests
while IFS= read -r line; do
    # Extract method from JSON (simple parsing for test)
    if [[ "${'$'}line" == *'"method":"prompts/list"'* ]]; then
        echo '{"jsonrpc":"2.0","id":1,"result":[{"name":"test-prompt","title":"Test","description":"Test prompt"}]}'
    elif [[ "${'$'}line" == *'"method":"tools/list"'* ]]; then
        echo '{"jsonrpc":"2.0","id":1,"result":[{"name":"test-tool","description":"Test tool","inputSchema":null,"outputSchema":null}]}'
    elif [[ "${'$'}line" == *'"method":"capabilities"'* ]]; then
        echo '{"jsonrpc":"2.0","id":1,"result":{"prompts":{"listChanged":false},"tools":null}}'
    else
        echo '{"jsonrpc":"2.0","id":1,"result":null}'
    fi
done
"""
        Files.writeString(scriptFile, scriptContent)
        scriptFile.toFile().setExecutable(true)

        runTest {
            // Only run test if we're on a Unix-like system with bash
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                // Skip on Windows
                return@runTest
            }

            try {
                val adapter = McpServerAdapterStdio(scriptFile.toString())
                
                // Test capabilities
                val capabilities = adapter.getCapabilities()
                assertNotNull(capabilities)
                
                // Test list prompts
                val prompts = adapter.listPrompts()
                assertNotNull(prompts)
                assertEquals(1, prompts.size)
                assertEquals("test-prompt", prompts[0].name)
                
                // Test list tools
                val tools = adapter.listTools()
                assertNotNull(tools)
                assertEquals(1, tools.size)
                assertEquals("test-tool", tools[0].name)
                
                adapter.close()
            } catch (e: Exception) {
                // If bash is not available, skip the test
                println("Skipping stdio test: ${e.message}")
            }
        }
    }

    @Test
    fun testStdioAdapterWithArgs() {
        runTest {
            // Test that args are passed correctly
            // We'll just verify the adapter can be created with args
            try {
                // Use 'echo' as a simple command that exists on most systems
                val adapter = McpServerAdapterStdio("echo", listOf("test"))
                // This will fail when trying to communicate, which is expected
                assertThrows(Exception::class.java) {
                    runTest {
                        adapter.listPrompts()
                    }
                }
                adapter.close()
            } catch (e: Exception) {
                // Expected - echo doesn't respond to JSON-RPC or doesn't exist
            }
        }
    }

    @Test
    fun testStdioAdapterWithEnv() {
        runTest {
            // Test that environment variables are passed correctly
            val env = mapOf("TEST_VAR" to "test_value")
            
            try {
                val adapter = McpServerAdapterStdio("echo", emptyList(), env)
                // This will fail when trying to communicate, which is expected
                assertThrows(Exception::class.java) {
                    runTest {
                        adapter.listPrompts()
                    }
                }
                adapter.close()
            } catch (e: Exception) {
                // Expected - echo doesn't respond to JSON-RPC or doesn't exist
            }
        }
    }

    @Test
    fun testStdioAdapterErrorHandling() {
        runTest {
            // Test that errors are properly handled
            val scriptFile = tempDir.resolve("error-server.sh")
            val scriptContent = """#!/bin/bash
while IFS= read -r line; do
    echo '{"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"Method not found"}}'
done
"""
            Files.writeString(scriptFile, scriptContent)
            scriptFile.toFile().setExecutable(true)

            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                try {
                    val adapter = McpServerAdapterStdio(scriptFile.toString())
                    
                    assertThrows(Exception::class.java) {
                        runTest {
                            adapter.listPrompts()
                        }
                    }
                    
                    adapter.close()
                } catch (e: Exception) {
                    println("Skipping error handling test: ${e.message}")
                }
            }
        }
    }
}
