package tri.ai.core.agent.impl

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import kotlinx.coroutines.flow.FlowCollector
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.TextChat
import tri.ai.core.TextChatMessage
import tri.ai.core.agent.AgentChatEvent
import tri.ai.core.agent.AgentChatResponse
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.BaseAgentChat
import tri.ai.pips.core.ExecContext
import tri.ai.pips.core.Executable
import tri.ai.pips.core.MAPPER
import tri.ai.prompt.fill
import tri.ai.tool.ToolExecutableResult
import tri.util.*

/**
 * Executes a user prompt using a set of tools using a planning operation.
 * Uses a [TextCompletion] to complete a Question/Thought/Action/Action Input/Observation/.../Thought/Final Answer loop.
 * Depending on the model, this approach may frequently repeat or fail to terminate.
 */
class ToolChainExecutor(val tools: List<Executable>) : BaseAgentChat() {

    companion object {
        private val TOOL_CHAIN_PROMPT = PROMPTS.get("tools/chain-executor")!!
    }

    val iterationLimit = 5
    val completionTokens = 500

    override suspend fun FlowCollector<AgentChatEvent>.sendMessageSafe(session: AgentChatSession, message: MultimodalChatMessage): AgentChatResponse {
        // require a single text message for input, otherwise trigger an error
        val question = ensureTextContent(message)

        // store and log message
        processMessage(session, message, this)
        val chat = findChat(session, this)

        val scratchpad = ExecContext().apply {
            vars["steps"] = MAPPER.createArrayNode()
        }
        var result: ToolExecutableResult? = null
        var iterations = 0
        while (result?.isTerminal != true && iterations++ < iterationLimit) {
            result = runExecChain(chat, templateForQuestion(question), tools, scratchpad)
        }
        val final = result?.finalResult ?: "I was unable to determine a final answer."
        val responseMessage = MultimodalChatMessage.text(MChatRole.Assistant, final)

        // store and log response, maybe update session name
        processMessageResponse(session, responseMessage, this)
        return AgentChatResponse(responseMessage, null, metadata = mapOf("model" to session.config.modelId))
    }

    private fun templateForQuestion(question: String) = TOOL_CHAIN_PROMPT.fill(
        "tools" to tools.joinToString("\n") { "${it.name}: ${it.description}" },
        "tool_names" to tools.joinToString(", ") { it.name },
        "input" to question,
        "agent_scratchpad" to "{{agent_scratchpad}}" // must be this exact string for replacement later
    )

    fun ExecContext.summary() = (vars["steps"] as ArrayNode).values().asSequence().joinToString("\n") { it.asText() }

    /** Runs a single step of an execution chain. */
    private suspend fun FlowCollector<AgentChatEvent>.runExecChain(chat: TextChat, promptTemplate: String, tools: List<Executable>, scratchpad: ExecContext): ToolExecutableResult {
        val prompt = promptTemplate.replace("{{agent_scratchpad}}", scratchpad.summary())

        val completion = chat.chat(listOf(TextChatMessage.user(prompt)), stop = listOf("Observation: "), tokens = completionTokens)
            .firstValue.textContent().trim()
            .replace("\n\n", "\n")
        emit(AgentChatEvent.Reasoning(completion))
        (scratchpad.vars["steps"] as ArrayNode).add(completion)

        val responseOp = completion.split("\n")
            .map { it.split(":", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }
        if ("Final Answer:" in completion) {
            val answer = completion.substringAfter("Final Answer:").trim()
            return ToolExecutableResult("", isTerminal = true, finalResult = answer)
        }

        val toolName = responseOp["Action"]?.trim()
        val tool = tools.find { it.name == toolName } ?: error("Tool $toolName not found.")
        val toolInput = responseOp["Action Input"]?.trim()

        scratchpad.vars[tool.name + " Input"] = TextNode(toolInput ?: "") // this is optional
        if (toolInput.isNullOrBlank()) {
            val fallback ="Tool input missing or malformed."
            emit(AgentChatEvent.Error(IllegalArgumentException(fallback)))
            scratchpad.vars[tool.name + " Result"] = TextNode(fallback) // this is optional
            return ToolExecutableResult("", false, fallback)
        }

        // execute tool
        val inputJson = MAPPER.createObjectNode().put("input", toolInput)
        emit(AgentChatEvent.UsingTool(tool.name, toolInput))
        val executionResult = tool.execute(inputJson, ExecContext())

        // log and return result
        val resultText = executionResult.get("result")?.asText() ?: ""
        emit(AgentChatEvent.Progress("Tool Result: $ANSI_CYAN$resultText$ANSI_RESET"))
        (scratchpad.vars["steps"] as ArrayNode).add("Observation: $resultText")
        scratchpad.vars[tool.name + " Result"] = TextNode(resultText) // this is optional

        return ToolExecutableResult(
            resultText,
            isTerminal = executionResult.get("isTerminal")?.asBoolean() ?: false,
            finalResult = executionResult.get("finalResult")?.asText()
        )
    }

}