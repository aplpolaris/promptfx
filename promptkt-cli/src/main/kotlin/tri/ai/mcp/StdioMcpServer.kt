package tri.ai.mcp

import tri.ai.prompt.PromptLibrary
import java.io.InputStream
import java.io.PrintStream

/** Local MCP server running on stdio -- switch out for a library when possible. */
class StdioMcpServer(private val library: PromptLibrary = PromptLibrary.INSTANCE) {

    private val server = LocalMcpServer(library)
    private val businessLogic = McpHandler(server)
    private val router = StdioJsonRpcMessageRouter(businessLogic)

    /** Start a blocking stdio loop reading JSON-RPC requests from [stream] and writing responses to [out]. */
    suspend fun startServer(stream: InputStream, out: PrintStream) {
        router.startServer(stream, out)
    }

    suspend fun close() {
        server.close()
    }

}