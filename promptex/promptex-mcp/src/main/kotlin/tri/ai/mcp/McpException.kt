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

/** Exception thrown when there is an MCP error. */
open class McpException(message: String, x: Throwable? = null) : Exception(message, x)

/** Exception thrown when SSE event data cannot be parsed. */
class McpSseParseException(message: String, val eventType: String?, val rawData: String, cause: Throwable? = null) 
    : McpException(message, cause)

/** Exception thrown when SSE event contains unexpected or invalid data. */
class McpSseInvalidDataException(message: String, val eventType: String?, val data: String) 
    : McpException(message)
