/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Interface for handling business logic of JSON-RPC requests.
 * Implementations should focus on the specific protocol (e.g., MCP) without
 * worrying about JSON-RPC message handling.
 */
interface JsonRpcHandler {
    /**
     * Handle a JSON-RPC request method with its parameters.
     * @param method The RPC method name
     * @param params The request parameters as JsonObject
     * @return JsonElement response or null if method not supported
     */
    suspend fun handleRequest(method: String?, params: JsonObject?): JsonElement?
}
