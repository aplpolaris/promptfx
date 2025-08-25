package tri.ai.research

object ResearchModels {}

/** Captures an information request. */
data class InformationRequest(
    val request: String
)

/** Captures a research plan for a given information request. */
data class ResearchPlan(
    val objectives: List<String>,
    val tasks: List<String>,
    val queries: List<String>,
    val deliverables: List<String>
)

/** Collection of resources gathered to address an information request/plan. */
data class ResearchPack(
    val resources: List<ResearchResource>
)

/** A single resource in a research pack. */
data class ResearchResource(
    val title: String
    // TODO
)

