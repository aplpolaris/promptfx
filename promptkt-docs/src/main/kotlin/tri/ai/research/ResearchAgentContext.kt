package tri.ai.research

import java.util.UUID

interface ResearchAgentContext {
    val sessionId: String
    val memory: MemoryStore
}

data class DefaultResearchAgentContext(
    override val sessionId: String = UUID.randomUUID().toString(),
    override val memory: MemoryStore = InMemoryStore()
) : ResearchAgentContext