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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tri.ai.core.MultimodalChatMessage
import tri.ai.mcp.tool.McpToolMetadata
import tri.ai.mcp.tool.McpToolResponse

class McpJsonRpcHandlerTest {

    @Test
    fun testMethodConstants() {
        // Verify all method name constants are properly defined
        assertEquals("initialize", McpJsonRpcHandler.METHOD_INITIALIZE)
        assertEquals("prompts/list", McpJsonRpcHandler.METHOD_PROMPTS_LIST)
        assertEquals("prompts/get", McpJsonRpcHandler.METHOD_PROMPTS_GET)
        assertEquals("tools/list", McpJsonRpcHandler.METHOD_TOOLS_LIST)
        assertEquals("tools/call", McpJsonRpcHandler.METHOD_TOOLS_CALL)
        assertEquals("resources/list", McpJsonRpcHandler.METHOD_RESOURCES_LIST)
        assertEquals("resources/templates/list", McpJsonRpcHandler.METHOD_RESOURCES_TEMPLATES_LIST)
        assertEquals("resources/read", McpJsonRpcHandler.METHOD_RESOURCES_READ)
        assertEquals("notifications/initialized", McpJsonRpcHandler.METHOD_NOTIFICATIONS_INITIALIZED)
        assertEquals("notifications/close", McpJsonRpcHandler.METHOD_NOTIFICATIONS_CLOSE)
        assertEquals("notifications/", McpJsonRpcHandler.METHOD_NOTIFICATIONS_PREFIX)
    }

    @Test
    fun testHandlerWithConstants() {
        runTest {
            val provider = createTestProvider()
            val handler = McpJsonRpcHandler(provider)

            // Test that the handler responds to method calls using the constants
            val initResult = handler.handleRequest(McpJsonRpcHandler.METHOD_INITIALIZE, null)
            assertNotNull(initResult, "Initialize should return a result")

            val promptsListResult = handler.handleRequest(McpJsonRpcHandler.METHOD_PROMPTS_LIST, null)
            assertNotNull(promptsListResult, "Prompts list should return a result")

            val toolsListResult = handler.handleRequest(McpJsonRpcHandler.METHOD_TOOLS_LIST, null)
            assertNotNull(toolsListResult, "Tools list should return a result")
        }
    }

    private fun createTestProvider() = object : McpProvider {
        override suspend fun getCapabilities() = McpCapabilities(
            prompts = McpCapability(listChanged = false),
            tools = McpCapability(listChanged = false)
        )

        override suspend fun listPrompts() = listOf(
            McpPrompt(
                name = "test-prompt",
                title = "Test Prompt",
                description = "A test prompt"
            )
        )

        override suspend fun getPrompt(name: String, args: Map<String, String>) =
            McpPromptResponse(
                description = "Test prompt response",
                messages = listOf(MultimodalChatMessage.Companion.user("Test message"))
            )

        override suspend fun listTools(): List<McpToolMetadata> = emptyList()

        override suspend fun getTool(name: String): McpToolMetadata? = null

        override suspend fun callTool(name: String, args: Map<String, Any?>) = McpToolResponse.error("Unsupported")

        override suspend fun listResources(): List<McpResource> = emptyList()

        override suspend fun listResourceTemplates(): List<McpResourceTemplate> = emptyList()

        override suspend fun readResource(uri: String): McpResourceResponse {
            throw McpException("Resource not found: $uri")
        }

        override suspend fun close() {
            // No-op for test
        }
    }
}
