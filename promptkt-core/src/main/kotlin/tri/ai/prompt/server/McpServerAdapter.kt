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
package tri.ai.prompt.server

/**
 * Interface for connecting to MCP prompt servers, supporting both local and remote connections.
 */
interface McpServerAdapter {
    
    /** List all available prompts from the server. */
    suspend fun listPrompts(): List<McpPrompt>
    
    /** Get a filled prompt with the given arguments. */
    suspend fun getPrompt(name: String, args: Map<String, String> = emptyMap()): McpGetPromptResponse
    
    /** Get server capabilities. */
    suspend fun getCapabilities(): McpServerCapabilities?
    
    /** Close the connection to the server. */
    suspend fun close()
}