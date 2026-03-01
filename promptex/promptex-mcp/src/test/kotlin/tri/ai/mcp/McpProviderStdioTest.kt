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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tri.ai.mcp.tool.McpContent
import tri.util.ANSI_BOLD
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import kotlin.collections.component1
import kotlin.collections.component2

class McpProviderStdioTest {

    @Test
    fun testInvalidCommand() {
        // Test that we get an appropriate error when trying to connect to a nonexistent command
        assertThrows(Exception::class.java) {
            runTest {
                val provider = McpProviderStdio("nonexistent-command-12345")
                provider.initialize()
                provider.close()
            }
        }
    }

    @Test
    fun testWithArgs() {
        runTest {
            // Test that args are passed correctly (will fail since echo doesn't respond properly)
            try {
                val provider = McpProviderStdio("echo", listOf("test"))

                // This should fail when trying to communicate
                try {
                    provider.listPrompts()
                    fail("Should have thrown an exception")
                } catch (e: McpException) {
                    println("testWithArgs error (expected): ${e.message}")
                }

                provider.close()
            } catch (e: Exception) {
                // Command might not exist on all systems
                println("testWithArgs - command not found (expected on some systems)")
            }
        }
    }

    @Test
    @Disabled("integration test requires connecting to a specific external MCP stdio server")
    fun testExternal() {
        val provider = McpProviderStdio(
            "C:\\Program Files\\nodejs\\npx.cmd",
            args = listOf("-y", "@modelcontextprotocol/server-everything")
        )

        runProviderTest(provider)

        runTest {
            println("-".repeat(40))
            val call = provider.callTool("echo", mapOf("message" to "Hello World")).content.first() as McpContent.Text
            println("-".repeat(20))
            println(call.text)
            provider.close()
        }
    }

    @Test
    @Disabled("integration test requires connecting to a specific external MCP stdio server")
    fun testExternalAlt() {
        val provider = McpProviderStdio(
            "C:\\Program Files\\nodejs\\npx.cmd",
            args = listOf("-y", "@modelcontextprotocol/server-everything")
        )
        runTest {
            val capabilities = provider.getCapabilities()
            println(capabilities)
            val tools = provider.listTools()
            println(tools)
            provider.close()
        }
    }

}

/** Execute a generic test of MCP protocol functions on the given provider. */
fun runProviderTest(provider: McpProvider) {
    runTest {
        println("-".repeat(40))
        try {
            provider.initialize()
            println(provider.getCapabilities())
        } catch (e: McpException) {
            println("getCapabilities error: ${e.message}")
        }
        println("-".repeat(40))
        println("PROMPTS:")
        try {
            println(provider.listPrompts().joinToString("\n") { " - ${it.name}: $it" })
        } catch (e: McpException) {
            println("listPrompts error: ${e.message}")
        }
        println("-".repeat(40))
        println("TOOLS:")
        try {
            println(provider.listTools().joinToString("\n") { " - ${it.name}: $it" })
        } catch (e: McpException) {
            println("listTools error: ${e.message}")
        }
        println("-".repeat(40))
        println("RESOURCES:")
        try {
            println(provider.listResources().joinToString("\n") { " - ${it.name}: $it" })
        } catch (e: McpException) {
            println("listResources error: ${e.message}")
        }
        println("-".repeat(40))
        println("RESOURCE TEMPLATES:")
        try {
            println(provider.listResourceTemplates().joinToString("\n") { " - ${it.name}: $it" })
        } catch (e: McpException) {
            println("listResourceTemplates error: ${e.message}")
        }

        println("-".repeat(40))
        println("TOOL DETAILS:")
        provider.listTools().forEach { tool ->
            println("${ANSI_BOLD}Name$ANSI_RESET: ${tool.name}")
            println("${ANSI_BOLD}Description$ANSI_RESET: $ANSI_GRAY${tool.description}$ANSI_RESET")
            printSchema(tool.inputSchema, "Input parameters")
            printSchema(tool.outputSchema, "Output properties")
            println("-".repeat(30))
        }
    }
}

private fun printSchema(schema: JsonNode?, label: String) {
    if (schema != null && schema is ObjectNode) {
        val properties = schema.get("properties") as? ObjectNode
        val required = schema.get("required")?.mapNotNull { it.asText() }?.toSet() ?: emptySet()
        if (properties != null) {
            println("${ANSI_BOLD}$label$ANSI_RESET:")
            properties.fields().forEach { (propName, propSchema) ->
                val propDesc = propSchema.get("description")?.asText() ?: "No description"
                val isRequired = if (required.contains(propName)) " (required)" else " (optional)"
                println("  - ${propName}$isRequired: $ANSI_GRAY${propDesc}$ANSI_RESET")
            }
        }
    }
}
