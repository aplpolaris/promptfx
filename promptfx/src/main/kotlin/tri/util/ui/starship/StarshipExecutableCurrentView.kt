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
package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.text.docs.FormattedText
import tri.promptfx.PromptFxDriver.sendInput
import tri.promptfx.PromptFxWorkspace

/** Executes the current view and returns the result as text. */
class StarshipExecutableCurrentView(val workspace: PromptFxWorkspace, val baseComponentTitle: String) : Executable {
    override val name = "starship/execute-view"
    override val description = "Executes the view that was active when Starship was launched and returns the result."
    override val version = "0.0.1"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    override suspend fun execute(input: JsonNode, context: ExecContext): JsonNode {
        val strInput = input.unwrappedTextValue()
        var text: FormattedText? = null
        workspace.sendInput(baseComponentTitle, strInput) { text = it }
        // TODO - propagate formatted text
        return TextNode.valueOf(text?.toString() ?: "")

    }
}
