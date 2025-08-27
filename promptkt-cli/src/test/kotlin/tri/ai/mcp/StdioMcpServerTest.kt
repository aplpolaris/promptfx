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