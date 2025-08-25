/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.MChatRole
import tri.ai.openai.OpenAiAdapter
import tri.ai.pips.agent.*
import tri.util.MIN_LEVEL_TO_LOG
import java.util.logging.Level
import kotlin.system.exitProcess

fun main(args: Array<String>) =
    AgentChatCli().main(args)

/**
 * Command-line interface for agent-based chat with contextual reasoning capabilities.
 * Uses the AgentChat API for streaming responses and progress monitoring.
 */
class AgentChatCli : CliktCommand(name = "chat-agent") {
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
    private val verbose by option("--verbose", help = "Verbose logging and progress").flag()
    private val systemMessage by option("--system", help = "System message for the agent")

    private val greeting
        get() = "You are chatting with $model using AgentChat. Say 'bye' to exit, 'sessions' to list sessions."

    private val api = DefaultAgentChatAPI()
    private lateinit var currentSession: AgentChatSession

    override fun run() {
        if (verbose) {
            println("Verbose logging enabled.")
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
        println(greeting)
        
        if (systemMessage != null) {
            println("System: $systemMessage")
        }
        
        println("Model: $model, Max Context: $maxContext, Temperature: $temperature")
        
        if (reasoning) {
            println("Reasoning mode enabled - interim thoughts will be shown.")
        }
        
        runBlocking {
            print("> ")
            var input = readln()
            while (input != "bye") {
                when (input.lowercase()) {
                    "sessions" -> {
                        listSessions()
                    }
                    "clear" -> {
                        clearSession()
                    }
                    "help" -> {
                        showHelp()
                    }
                    else -> {
                        if (input.isNotBlank()) {
                            sendMessage(input)
                        }
                    }
                }
                print("> ")
                input = readln()
            }
        }
        println("Goodbye!")
        exitProcess(0)
    }

    private suspend fun sendMessage(userInput: String) {
        val message = MultimodalChatMessage.text(MChatRole.User, userInput)
        val operation = api.sendMessage(currentSession, message)
        
        try {
            operation.events.collect { event ->
                when (event) {
                    is AgentChatEvent.Progress -> {
                        if (verbose) {
                            println("[Progress] ${event.message}")
                        }
                    }
                    is AgentChatEvent.Reasoning -> {
                        if (reasoning) {
                            println("[Thinking] ${event.reasoning}")
                        }
                    }
                    is AgentChatEvent.StreamingToken -> {
                        print(event.token)
                    }
                    is AgentChatEvent.Response -> {
                        val responseText = event.response.message.content?.firstOrNull()?.text ?: "[No response]"
                        if (!verbose && !reasoning) {
                            // If we haven't been printing progress/streaming, print the full response
                            println(responseText)
                        } else {
                            // If we've been printing tokens, just add a newline
                            println()
                        }
                        
                        if (event.response.reasoning != null) {
                            println("[Reasoning] ${event.response.reasoning}")
                        }
                    }
                    is AgentChatEvent.Error -> {
                        println("Error: ${event.error.message}")
                        if (verbose) {
                            event.error.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
        }
    }

    private fun listSessions() {
        val sessions = api.listSessions()
        if (sessions.isEmpty()) {
            println("No saved sessions found.")
        } else {
            println("Sessions:")
            sessions.forEach { session ->
                val currentIndicator = if (session.sessionId == currentSession.sessionId) "*" else " "
                println("$currentIndicator ${session.name} (${session.messageCount} messages) - ${session.lastModified}")
                session.lastMessagePreview?.let {
                    println("   Last: $it")
                }
            }
        }
    }

    private fun clearSession() {
        val config = currentSession.config
        currentSession = api.createSession(config)
        println("Session cleared. Starting new conversation.")
    }

    private fun showHelp() {
        println("Commands:")
        println("  bye      - Exit the chat")
        println("  sessions - List all sessions")  
        println("  clear    - Clear current session and start new")
        println("  help     - Show this help")
        println("\nOptions:")
        println("  --model      Multimodal model to use")
        println("  --maxContext Maximum context messages") 
        println("  --temperature Response temperature")
        println("  --reasoning  Enable reasoning mode")
        println("  --verbose    Show progress and detailed output")
        println("  --system     Set system message")
    }
}