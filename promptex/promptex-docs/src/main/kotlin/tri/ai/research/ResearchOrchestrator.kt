/*-
 * #%L
 * tri.promptfx:promptex-docs
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
package tri.ai.research

import tri.ai.core.CompletionBuilder
import tri.ai.core.TextChat
import tri.ai.prompt.PromptLibrary

/**
 * Orchestrates a multi-agent research workflow with the following stages:
 * 1. **Planner Agent** – generates a structured research plan from an [InformationRequest].
 * 2. **Research Agent** – produces research questions and findings based on the plan.
 * 3. **Writing Agent** – generates a report outline, then writes a full draft.
 * 4. **Review Agent** – evaluates the draft for quality and returns review notes.
 *
 * Each stage delegates to prompts in the built-in prompt library
 * (`research-report/` and `research-quality/` groups).
 */
class ResearchOrchestrator(val chat: TextChat, val maxTokens: Int = 2000) {

    private val library = PromptLibrary.INSTANCE

    /**
     * Stage 1 – Planner Agent.
     * Analyzes the [request] and returns a detailed, actionable research plan as plain text.
     */
    suspend fun generatePlan(request: String): String =
        executePrompt("research-report/planner", "request" to request)

    /**
     * Stage 2 – Research Agent (two-tier).
     * First tier: generates research questions for [topic].
     * Second tier: identifies the best data sources and search queries for the topic.
     * The combined output forms the research pack for the writing agent.
     */
    suspend fun conductResearch(topic: String, plan: String): String {
        val questions = executePrompt("research-report/questions", "topic" to topic)
        val sources = executePrompt("research-report/sources", "topic" to topic, "plan" to plan)
        return buildString {
            appendLine("=== Research Questions ===")
            appendLine(questions)
            appendLine()
            appendLine("=== Recommended Data Sources and Search Strategy ===")
            appendLine(sources)
            appendLine()
            appendLine("=== Research Plan Summary ===")
            appendLine(plan)
        }
    }

    /**
     * Stage 3a – Writing Agent (outline step).
     * Generates a structured report outline for [topic].
     */
    suspend fun generateOutline(topic: String): String =
        executePrompt("research-report/outline", "topic" to topic)

    /**
     * Stage 3b – Writing Agent (draft step).
     * Combines [research] findings and [outline] to produce a full draft report for [topic].
     */
    suspend fun writeDraft(topic: String, research: String, outline: String): String =
        executePrompt(
            "research-report/draft",
            "topic" to topic,
            "research" to research,
            "outline" to outline
        )

    /**
     * Stage 4 – Review Agent.
     * Evaluates [report] against core research quality principles and returns review notes.
     */
    suspend fun reviewReport(report: String): String =
        executePrompt("research-quality/final-eval", "input" to report)

    // Internal helper: looks up a prompt by id (bare id without version) and executes it.
    private suspend fun executePrompt(promptId: String, vararg params: Pair<String, Any>): String {
        val prompt = library.get(promptId)
            ?: error("Prompt '$promptId' not found in the prompt library.")
        return CompletionBuilder()
            .prompt(prompt)
            .params(*params)
            .tokens(maxTokens)
            .execute(chat)
            .firstValue.textContent() ?: ""
    }
}
