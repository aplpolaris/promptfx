package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.agent.createObject
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
        return createObject(VIEW_RESULT_KEY, text.toString())

    }

    companion object {
        const val VIEW_RESULT_KEY = "viewResult"
    }
}