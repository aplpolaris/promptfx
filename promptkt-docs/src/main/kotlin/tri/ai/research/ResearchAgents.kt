package tri.ai.research

import tri.ai.pips.api.AgentExecutable

val PROJECT_MANAGER_AGENT = AgentExecutable(
    name = "Project Manager Agent",
    description = "Plans and manages AI research projects based around a user's information request.",
    version = "1.0",
    null,
    null,
    listOf()
)

val RESEARCH_PACK_AGENT = AgentExecutable(
    name = "Research Pack Agent",
    description = "Gathers and curates a collection of resources to address a research plan.",
    version = "1.0",
    null,
    null,
    listOf()
)

val RESEARCH_PLANNER_AGENT = AgentExecutable(
    name = "Research Planner Agent",
    description = "Creates a detailed research plan based on an information request.",
    version = "1.0",
    null,
    null,
    listOf()
)
