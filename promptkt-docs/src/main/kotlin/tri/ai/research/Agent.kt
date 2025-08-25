package tri.ai.research

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.pips.core.Executable

/** Base class for an AI agent that can be invoked as a PExecutable. */
abstract class Agent(
    override val name: String,
    override val version: String,
    override val inputSchema: JsonNode?,
    override val outputSchema: JsonNode?
) : Executable