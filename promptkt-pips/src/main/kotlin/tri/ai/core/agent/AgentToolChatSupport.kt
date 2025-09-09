package tri.ai.core.agent

import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.tool.Executable

/** Partial implementation of [AgentChat] that supports using tools ([tri.ai.core.tool.Executable]s). */
abstract class AgentToolChatSupport(val tools: List<Executable>) : AgentChatSupport() {

    /** Logs tool usage. */
    suspend fun FlowCollector<AgentChatEvent>.logToolUsage() {
        emit(AgentChatEvent.Progress("Using tools: ${tools.joinToString(", ") { it.name }}"))
    }

}