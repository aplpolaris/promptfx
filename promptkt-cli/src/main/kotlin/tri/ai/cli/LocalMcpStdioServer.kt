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
package tri.ai.cli

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import tri.ai.core.MChatMessagePart
import tri.ai.core.MChatRole
import tri.ai.pips.core.MAPPER
import tri.ai.prompt.PromptLibrary
import tri.ai.prompt.server.LocalMcpServer
import tri.ai.prompt.server.McpGetPromptResponse
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream

/** Local MCP server running on stdio -- switch out for a library when possible. */
class LocalMcpStdioServer(private val library: PromptLibrary = PromptLibrary.INSTANCE) {

    private val server = LocalMcpServer(library)
    private val serializer = JsonRpcSerializer()
    private val businessLogic = McpBusinessLogic(server, serializer)
    private val router = JsonRpcMessageRouter(serializer, businessLogic)

    /** Start a blocking stdio loop reading JSON-RPC requests from [stream] and writing responses to [out]. */
    suspend fun startServer(stream: InputStream, out: PrintStream) {
        router.startServer(stream, out)
    }

    suspend fun close() {
        server.close()
    }

}
