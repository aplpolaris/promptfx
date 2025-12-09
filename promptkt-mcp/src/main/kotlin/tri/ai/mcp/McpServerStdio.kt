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

import java.io.InputStream
import java.io.PrintStream

/**
 * MCP server that runs over stdio -- switch out for a library when possible.
 * Uses [McpServerAdapter] for the underlying server implementation,
 * allowing the server to be defined in-memory or to mirror a remote server.
 */
class McpServerStdio(private val server: McpServerAdapter) {

    private val businessLogic = McpServerHandler(server)
    private val router = StdioJsonRpcMessageRouter(businessLogic)

    /** Start a blocking stdio loop reading JSON-RPC requests from [stream] and writing responses to [out]. */
    suspend fun startServer(stream: InputStream, out: PrintStream) {
        router.startServer(stream, out)
    }

    suspend fun close() {
        server.close()
        router.close()
    }

}

