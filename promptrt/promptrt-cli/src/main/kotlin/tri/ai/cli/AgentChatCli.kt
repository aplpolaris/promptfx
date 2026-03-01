/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.cli

import com.aallam.openai.api.logging.LogLevel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parsers.CommandLineParser
import kotlinx.coroutines.runBlocking
import tri.ai.core.MChatRole
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.agent.AgentChatConfig
import tri.ai.core.agent.AgentChatSession
import tri.ai.core.agent.AgentFlowLogger
import tri.ai.core.agent.api.AgentChatAPI
import tri.ai.core.agent.api.DefaultAgentChatAPI
import tri.ai.openai.OpenAiAdapter
import tri.util.*
import java.time.LocalDateTime
import java.util.logging.Level
import kotlin.system.exitProcess

/**
 * Command-line interface for agent-based chat with contextual reasoning capabilities.
 * Uses the AgentChat API for streaming responses and progress monitoring.
 */
class AgentChatCli : CliktCommand(name = "chat-agent") {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AgentChatCli().main(args)
        }
    }

    private val model by option("--model", help = "Multimodal model to use (default gpt-4o-mini)")
        .default("gpt-4o-mini")
    private val maxContext by option("--maxContext", help = "Maximum context messages (default 20)")
        .int()
        .default(20)
    private val temperature by option("--temperature", help = "Response temperature (default 0.7)")
        .double()
        .default(0.7)
    private val maxTokens by option("--maxTokens", help = "Maximum response tokens (default 4000)")
        .int()
        .default(4000)
    private val reasoning by option("--reasoning", help = "Enable reasoning mode").flag()
    private val verbose by option("--verbose", help = "Verbose logging and progress").flag(default = false)
    private val systemMessage by option("--system", help = "System message for the agent")

    private val greeting
        get() = "You are chatting with $model using AgentChat. Say 'bye' to exit, '/sessions' to list sessions, '/help' for commands."

    private val api: AgentChatAPI = DefaultAgentChatAPI()
    private lateinit var currentSession: AgentChatSession

    override fun run() {
        if (verbose) {
            printlnInfo("Verbose logging enabled.")
            MIN_LEVEL_TO_LOG = Level.FINE
        } else {
            MIN_LEVEL_TO_LOG = Level.WARNING
            OpenAiAdapter.INSTANCE.settings.logLevel = LogLevel.None
        }

        // Create session with configuration
        val config = AgentChatConfig(
            modelId = model,
            maxContextMessages = maxContext,
            systemMessage = systemMessage,
            temperature = temperature,
            maxTokens = maxTokens,
            enableReasoningMode = reasoning
        )
        
        currentSession = api.createSession(config)
        printlnInfo(greeting)
        
        if (systemMessage != null) {
            printlnInfo("System: $systemMessage")
        }

        printlnInfo("Model: $model, Max Context: $maxContext, Temperature: $temperature")
        
        if (reasoning) {
            printlnInfo("Reasoning mode enabled - interim thoughts will be shown.")
        }
        
        runBlocking {
            print("> ")
            var input = readln()
            while (input != "bye") {
                when {
                    input.lowercase() == "/sessions" -> listSessions()
                    input.lowercase() == "/clear" -> clearSession()
                    input.lowercase() == "/help" -> showHelp()
                    input.lowercase() == "/api" -> showApiInfo()
                    input.lowercase() == "/save" -> saveCurrentSession()
                    input.lowercase() == "/info" -> showSessionInfo()
                    input.lowercase().startsWith("/switch ") ->
                        switchSession(input.removePrefix("/switch ").trim())
                    input.lowercase().startsWith("/load ") ->
                        loadSessionById(input.removePrefix("/load ").trim())
                    input.lowercase().startsWith("/delete ") ->
                        deleteSessionById(input.removePrefix("/delete ").trim())
                    input.startsWith("/") -> {
                        printlnError("Unknown command: $input. Type '/help' for available commands.")
                    }
                    input.isNotBlank() -> sendMessage(input)
                }
                print("> ")
                input = readln()
            }
        }
        printlnResponse("Goodbye!")
        exitProcess(0)
    }

    private suspend fun sendMessage(userInput: String) {
        val message = MultimodalChatMessage.text(MChatRole.User, userInput)
        val operation = api.sendMessage(currentSession, message)
        operation.events.collect(AgentFlowLogger(verbose))
    }

    private fun listSessions() {
        val sessions = api.listSessions()
        if (sessions.isEmpty()) {
            printlnInfo("No saved sessions found.")
        } else {
            printlnInfo("Sessions:")
            sessions.forEach { session ->
                val currentIndicator = if (session.sessionId == currentSession.sessionId) "*" else " "
                printlnInfo("$currentIndicator ${session.name} (${session.messageCount} messages) - ${session.lastModified.print} - ${session.sessionId}")
                session.lastMessagePreview?.let {
                    printlnInfo("     Last: $it")
                }
            }
        }
    }

    //region PRINT FUNCTIONS

    private fun printlnResponse(text: String) {
        kotlin.io.println("${ANSI_LIGHTBLUE}$text$ANSI_RESET")
    }

    private fun printlnInfo(text: String) {
        kotlin.io.println("${ANSI_GRAY}$text$ANSI_RESET")
    }

    private fun printlnError(text: String?) {
        kotlin.io.println("${ANSI_RED}ERROR: $text$ANSI_RESET")
    }

    // format as HH:mm:ss if today, otherwise YYYY-MM-DD HH:mm:ss
    private val LocalDateTime.print
        get() = when {
            toLocalDate() == LocalDateTime.now().toLocalDate() -> toLocalTime().withNano(0).toString()
            else -> toString().substring(0, 19)
        }

    //endregion

    private fun clearSession() {
        val config = currentSession.config
        currentSession = api.createSession(config)
        printlnInfo("Session cleared. Starting new conversation.")
    }

    private fun showHelp() {
        printlnInfo("""
            Commands:
                bye           - Exit the chat
                /sessions     - List all sessions
                /clear        - Clear current session and start new
                /help         - Show this help
                /api          - Show API information
                /switch <id>  - Switch to a different session by ID
                /load <id>    - Load a session by ID
                /delete <id>  - Delete a session by ID
                /save         - Save current session
                /info         - Show current session information
            Options:
                --model       Multimodal model to use
                --maxContext  Maximum context messages
                --temperature Response temperature
                --reasoning   Enable reasoning mode
                --verbose     Show progress and detailed output (default on)
                --system      Set system message
        """.trimIndent())
    }

    private fun showApiInfo() {
        printlnInfo("AgentChat API Information:")
        printlnInfo("  API Implementation: ${api.javaClass.simpleName}")
        printlnInfo("  Model: $model")
        printlnInfo("  Max Context: $maxContext")
        printlnInfo("  Temperature: $temperature")
        printlnInfo("  Max Tokens: $maxTokens")
        printlnInfo("  Reasoning Mode: $reasoning")
        printlnInfo("  System Message: ${systemMessage ?: "None"}")
        printlnInfo("  Current Session: ${currentSession.sessionId}")
        printlnInfo("  Session Name: ${currentSession.name}")
        printlnInfo("  Messages in Session: ${currentSession.messages.size}")
    }

    private fun switchSession(sessionId: String) {
        val session = api.loadSession(sessionId)
        if (session != null) {
            currentSession = session
            printlnInfo("Switched to session: ${session.name} (${session.sessionId})")
            printlnInfo("Messages: ${session.messages.size}")
        } else {
            printlnInfo("Session not found: $sessionId")
        }
    }

    private fun loadSessionById(sessionId: String) {
        val session = api.loadSession(sessionId)
        if (session != null) {
            currentSession = session
            printlnInfo("Loaded session: ${session.name}")
            printlnInfo("Session ID: ${session.sessionId}")
            printlnInfo("Created: ${session.createdAt.print}")
            printlnInfo("Last Modified: ${session.lastModified.print}")
            printlnInfo("Messages: ${session.messages.size}")
            if (session.messages.isNotEmpty()) {
                printlnInfo("Last message preview: ${session.messages.last().content?.firstOrNull()?.text?.take(50) ?: "N/A"}...")
            }
        } else {
            printlnInfo("Session not found: $sessionId")
        }
    }

    private fun deleteSessionById(sessionId: String) {
        if (sessionId == currentSession.sessionId) {
            printlnInfo("Cannot delete the current active session. Switch to another session first.")
            return
        }
        val deleted = api.deleteSession(sessionId)
        if (deleted) {
            printlnInfo("Session deleted: $sessionId")
        } else {
            printlnInfo("Session not found or could not be deleted: $sessionId")
        }
    }

    private fun saveCurrentSession() {
        val sessionId = api.saveSession(currentSession)
        printlnInfo("Session saved: $sessionId")
        printlnInfo("Session name: ${currentSession.name}")
    }

    private fun showSessionInfo() {
        printlnInfo("Current Session Information:")
        printlnInfo("  Session ID: ${currentSession.sessionId}")
        printlnInfo("  Name: ${currentSession.name}")
        printlnInfo("  Created: ${currentSession.createdAt}")
        printlnInfo("  Last Modified: ${currentSession.lastModified}")
        printlnInfo("  Messages: ${currentSession.messages.size}")
        printlnInfo("  Configuration:")
        printlnInfo("    Model: ${currentSession.config.modelId}")
        printlnInfo("    Max Context: ${currentSession.config.maxContextMessages}")
        printlnInfo("    Temperature: ${currentSession.config.temperature}")
        printlnInfo("    Max Tokens: ${currentSession.config.maxTokens}")
        printlnInfo("    Enable Tools: ${currentSession.config.enableTools}")
        printlnInfo("    Reasoning Mode: ${currentSession.config.enableReasoningMode}")
        printlnInfo("    System Message: ${currentSession.config.systemMessage ?: "None"}")
    }
}
