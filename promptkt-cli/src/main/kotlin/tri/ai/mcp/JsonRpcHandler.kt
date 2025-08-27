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