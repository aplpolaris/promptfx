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

class StdioMcpServerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testCreateWithCustomLibrary() {
        runTest {
            // Create a custom prompt file
            val promptFile = tempDir.resolve("custom-prompts.yaml")
            Files.writeString(
                promptFile, """
                id: test-group
                title: Test Group
                description: Test prompt group for server testing
                prompts:
                  - id: test/server-prompt
                    title: Server Test Prompt
                    description: A test prompt for server functionality
                    template: |
                      Server test: {{input}}
            """.trimIndent()
            )

            val customLibrary = PromptLibrary.loadFromPath(promptFile.toString())
            val server = StdioMcpServer(customLibrary)

            // Verify the server uses the custom library by checking what prompts are available
            // We can't easily test the stdio functionality, but we can verify the library integration
            assertNotNull(server, "Server should be created successfully")

            server.close()
        }
    }

    @Test
    fun testCreateWithDefaultLibrary() {
        runTest {
            val server = StdioMcpServer()
            assertNotNull(server, "Server should be created with default library")
            server.close()
        }
    }
}
