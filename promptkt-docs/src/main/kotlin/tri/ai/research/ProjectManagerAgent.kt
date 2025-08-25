package tri.ai.research

import com.fasterxml.jackson.databind.JsonNode
import tri.ai.pips.core.ExecContext

/** Project Manager Agent for planning and managing AI research projects. */
class ProjectManagerAgent : Agent(
    name = "Project Manager Agent",
    // description = "Plans and manages AI research projects based around a user's information request."
    version = "1.0",
    inputSchema = null,
    outputSchema = null
) {
    override suspend fun execute(
        input: JsonNode,
        context: ExecContext
    ): JsonNode {
        // TODO - set up workflow
        TODO()
    }
}

/** Research Planner Agent for creating research plans. */
class ResearchPlannerAgent : Agent(
    name = "Research Planner Agent",
    // description = "Creates a research plan to address a user's information request."
    version = "1.0",
    inputSchema = null,
    outputSchema = null
) {
    override suspend fun execute(
        input: JsonNode,
        context: ExecContext
    ): JsonNode {
        // TODO - implement planning logic
        TODO()
    }
}

/** Research Executor Agent for executing research plans. */
class ResearchExecutorAgent : Agent(
    name = "Research Executor Agent",
    // description = "Executes a research plan to gather information and insights."
    version = "1.0",
    inputSchema = null,
    outputSchema = null
) {
    override suspend fun execute(
        input: JsonNode,
        context: ExecContext
    ): JsonNode {
        // TODO - implement execution logic
        TODO()
    }

