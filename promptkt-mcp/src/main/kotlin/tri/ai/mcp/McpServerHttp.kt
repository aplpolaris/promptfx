package tri.ai.mcp

/**
 * MCP server that runs over http -- switch out for a library when possible.
 * Uses [McpServerAdapter] for the underlying server implementation,
 * allowing the server to be defined in-memory or to mirror a remote server.
 */
class McpServerHttp(private val server: McpServerAdapter) {

    private val businessLogic = McpServerHandler(server)

    /** Start the HTTP server. */
    suspend fun startServer() {
        TODO()
    }

    suspend fun close() {
        server.close()
        // router.close()
    }

}