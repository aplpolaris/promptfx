package tri.ai.mcp.http

import tri.ai.mcp.McpServerAdapter
import tri.ai.mcp.McpServerHandler

/**
 * MCP server that runs over http -- switch out for a library when possible.
 * Uses [tri.ai.mcp.McpServerAdapter] for the underlying server implementation,
 * allowing the server to be defined in-memory or to mirror a remote server.
 */
class McpServerHttp(private val server: McpServerAdapter, private val port: Int = 8080) {

    private val businessLogic = McpServerHandler(server)
    private val router = HttpJsonRpcMessageRouter(businessLogic)

    /** Start the HTTP server. */
    fun startServer() {
        router.startServer(port)
    }

    suspend fun close() {
        server.close()
        router.close()
    }

}