/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.mcp

import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [McpResourceView]. */
class McpResourcePlugin : NavigableWorkspaceViewImpl<McpResourceView>("MCP", "Resources", type = McpResourceView::class)

/** View and try out MCP server prompts. */
class McpResourceView : AiTaskView("MCP Resources", "View and test resources for configured MCP servers.") {
    override suspend fun processUserInput() = AiPipelineResult.todo()
}
