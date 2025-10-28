package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.core.agent.createObject
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable

/** Executes the current view and returns the result as text. */
class StarshipExecutableThumbnails : Executable {
    override val name = "starship/execute-view-thumbnails"
    override val description = "Returns thumbnails generated while executing a view."
    override val version = "0.0.1"
    override val inputSchema: JsonNode? = null
    override val outputSchema: JsonNode? = null

    override suspend fun execute(input: com.fasterxml.jackson.databind.JsonNode, context: tri.ai.core.tool.ExecContext) =
        createObject("viewResult", "This is a simulated result of executing the Starship view.")
}