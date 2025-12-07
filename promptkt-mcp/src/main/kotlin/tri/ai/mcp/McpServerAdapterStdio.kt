package tri.ai.mcp

/**
 * Remote MCP server adapter that connects to external MCP servers via stdio.
 */
class McpServerAdapterStdio() : McpServerAdapter {

    override suspend fun getCapabilities() = TODO()

    override suspend fun listPrompts() = TODO()
    override suspend fun getPrompt(name: String, args: Map<String, String>) = TODO()

    override suspend fun listTools() = TODO()
    override suspend fun getTool(name: String) = TODO()
    override suspend fun callTool(name: String, args: Map<String, String>) = TODO()

    override suspend fun close() = TODO()

}