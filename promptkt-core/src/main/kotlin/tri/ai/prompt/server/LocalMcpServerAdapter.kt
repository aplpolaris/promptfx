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

import tri.ai.prompt.PromptLibrary

/**
 * Local MCP server adapter that uses the in-process McpPromptServer.
 */
class LocalMcpServerAdapter(
    private val library: PromptLibrary = PromptLibrary()
) : McpServerAdapter {
    
    private val server = McpPromptServer().apply {
        this.library = this@LocalMcpServerAdapter.library
    }
    
    override suspend fun listPrompts(): List<McpPrompt> {
        return server.listPrompts()
    }
    
    override suspend fun getPrompt(name: String, args: Map<String, String>): McpGetPromptResponse {
        return server.getPrompt(name, args)
    }
    
    override suspend fun getCapabilities(): McpServerCapabilities {
        return McpServerCapabilities(
            prompts = McpServerPromptCapability(listChanged = false)
        )
    }
    
    override suspend fun close() {
        // Nothing to close for local adapter
    }
}