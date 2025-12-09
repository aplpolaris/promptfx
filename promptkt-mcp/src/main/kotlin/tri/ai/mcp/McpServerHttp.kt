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
