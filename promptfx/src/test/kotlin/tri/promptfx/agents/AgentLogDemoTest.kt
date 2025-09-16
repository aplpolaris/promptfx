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
package tri.promptfx.agents

import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tri.ai.core.MultimodalChatMessage
import tri.ai.core.agent.*

/**
 * Demonstration of the agent flow logging functionality.
 * This shows how agent events are formatted in the UI log.
 */
class AgentLogDemoTest {

    @Test
    fun demoAgentFlowLogging() = runBlocking {
        println("=== Agent Flow Logging Demo ===")
        println("This demonstrates how agent events appear in the UI log area.")
        println()
        
        // Create a simple mock log property
        val agentLog = SimpleStringProperty("")
        
        // Simple collector that mimics the UI behavior
        val collector = object {
            suspend fun emit(event: AgentChatEvent) {
                val logEntry = when (event) {
                    is AgentChatEvent.User -> formatLogEntry("USER", event.message)
                    is AgentChatEvent.Progress -> formatLogEntry("PROGRESS", event.message)
                    is AgentChatEvent.Reasoning -> formatLogEntry("REASONING", event.reasoning)
                    is AgentChatEvent.PlanningTask -> formatLogEntry("TASK", "${event.taskId}: ${event.description}")
                    is AgentChatEvent.UsingTool -> formatLogEntry("TOOL-IN", "${event.toolName}: ${event.input}")
                    is AgentChatEvent.ToolResult -> formatLogEntry("TOOL-OUT", "${event.toolName}: ${event.result}")
                    is AgentChatEvent.StreamingToken -> event.token
                    is AgentChatEvent.Response -> {
                        val responseText = event.response.message.content?.firstOrNull()?.text ?: "[No response]"
                        val reasoning = event.response.reasoning
                        formatLogEntry("FINAL", responseText) + 
                        if (reasoning != null) "\n${formatLogEntry("REASONING", reasoning)}" else ""
                    }
                    is AgentChatEvent.Error -> formatLogEntry("ERROR", event.error.message ?: "Unknown error")
                }
                
                if (event is AgentChatEvent.StreamingToken) {
                    agentLog.value = agentLog.value + logEntry
                } else {
                    agentLog.value = agentLog.value + (if (agentLog.value.isNotEmpty()) "\n" else "") + logEntry
                }
            }
            
            private fun formatLogEntry(label: String, text: String): String {
                val trimmed = text.trim()
                return if ("\n" in trimmed) {
                    "[$label] ${trimmed.lines().first()}\n${trimmed.lines().drop(1).joinToString("\n") { "         $it" }}"
                } else {
                    "[$label] $trimmed"
                }
            }
        }
        
        // Simulate a typical agent workflow
        collector.emit(AgentChatEvent.User("Help me write a professional email"))
        collector.emit(AgentChatEvent.Progress("Starting agent workflow..."))
        collector.emit(AgentChatEvent.Reasoning("I need to gather information about the email requirements"))
        collector.emit(AgentChatEvent.PlanningTask("task-1", "Identify email purpose and audience"))
        collector.emit(AgentChatEvent.UsingTool("text_analyzer", """
            Analyze this request:
            - Purpose: Professional email
            - Audience: Unknown
            - Content: Not specified
        """.trimIndent()))
        collector.emit(AgentChatEvent.ToolResult("text_analyzer", "Email requires: recipient info, subject, purpose"))
        collector.emit(AgentChatEvent.Progress("Generating email template..."))
        collector.emit(AgentChatEvent.UsingTool("email_generator", "Create professional email template"))
        collector.emit(AgentChatEvent.ToolResult("email_generator", """
            Subject: [Your Subject Here]
            
            Dear [Recipient Name],
            
            I hope this email finds you well.
            
            [Your message content here]
            
            Best regards,
            [Your name]
        """.trimIndent()))
        
        // Simulate streaming response
        val tokens = listOf("Here", " is", " a", " professional", " email", " template", " for", " you", ".")
        tokens.forEach { collector.emit(AgentChatEvent.StreamingToken(it)) }
        
        // Final response using real AgentChatResponse
        val mockResponse = AgentChatResponse(
            message = MultimodalChatMessage.user("I've created a professional email template for you. The template includes all the standard elements and can be customized for your specific needs."),
            reasoning = "Provided a complete email template with placeholders for customization"
        )
        collector.emit(AgentChatEvent.Response(mockResponse))
        
        // Display the final log
        println("Final Agent Log Output:")
        println("=" * 50)
        println(agentLog.value)
        println("=" * 50)
        println()
        println("This log would appear in the 'Agent Log' tab of the AgenticView UI.")
        println("The final result would appear in the 'Final Result' tab.")
    }
    
    private operator fun String.times(n: Int) = this.repeat(n)
}
