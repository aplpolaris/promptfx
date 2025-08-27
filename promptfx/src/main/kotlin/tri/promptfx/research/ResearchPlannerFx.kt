/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.research

import tornadofx.runLater
import tri.ai.core.TextCompletion
import tri.ai.core.instructTask
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskList
import tri.ai.pips.task
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxGlobals.lookupPrompt
import tri.util.info

/** Orchestrates the research workflow with multiple agents. */
class ResearchPlannerFx(private val state: ResearchWorkflowState) {

    companion object {
        const val PLANNER_PROMPT_ID = "research-report/planner@1.0.0"
        const val QUESTIONS_PROMPT_ID = "research-report/questions@1.0.0"
        const val OUTLINE_PROMPT_ID = "research-report/outline@1.0.0"
        const val DRAFT_PROMPT_ID = "research-report/draft@1.0.0"
        const val REVIEW_PROMPT_ID = "research-report/review-edit@1.0.0"
    }

    /** Creates a task list for the complete research workflow. */
    fun createResearchWorkflow(
        infoRequest: InfoRequest,
        completionEngine: TextCompletion,
        maxTokens: Int,
        temp: Double
    ): AiTaskList<WrittenReport> {
        
        return AiTaskList("research-workflow", "Complete research workflow") {
            runLater { 
                state.currentPhase.set(ResearchPhase.PLANNING)
                state.currentStatus.set("Analyzing research request...")
            }
            info<ResearchPlannerFx>("Starting research workflow for: ${infoRequest.request}")
            
            // Create a simple mock workflow result for now
            val mockPlan = ResearchProjectPlan(
                objectives = listOf("Understand the topic", "Gather information", "Analyze findings"),
                questions = listOf("What is the current state?", "What are the key challenges?"),
                methodology = listOf("Literature review", "Analysis"),
                tasks = listOf(
                    ResearchTask("1", "Research", "Conduct research", "Research", TaskPriority.HIGH, "2 hours"),
                    ResearchTask("2", "Write", "Write report", "Writing", TaskPriority.HIGH, "1 hour")
                ),
                timeline = "3 hours total",
                successCriteria = listOf("Complete coverage", "Clear conclusions")
            )
            
            val mockPack = ResearchPack(
                findings = listOf(
                    ResearchFinding("Topic Overview", "Overview of the topic", "Source 1"),
                    ResearchFinding("Key Points", "Important points discovered", "Source 2")
                ),
                sources = listOf("Source 1", "Source 2"),
                summary = "Research completed with 2 key findings"
            )
            
            val mockReport = WrittenReport(
                title = "Research Report: ${infoRequest.request}",
                sections = listOf(
                    ReportSection("Introduction", "This report examines ${infoRequest.request}.", 1),
                    ReportSection("Findings", "Key findings from the research.", 1),
                    ReportSection("Conclusion", "Summary and conclusions.", 1)
                ),
                outline = "1. Introduction\n2. Findings\n3. Conclusion",
                fullText = "# Research Report: ${infoRequest.request}\n\n## Introduction\nThis report examines ${infoRequest.request}.\n\n## Findings\nKey findings from the research.\n\n## Conclusion\nSummary and conclusions.",
                citations = listOf("Source 1", "Source 2")
            )
            
            runLater {
                state.researchPlan.set(mockPlan)
                state.currentPhase.set(ResearchPhase.RESEARCH)
                state.currentStatus.set("Research phase...")
                state.researchPack.set(mockPack)
                state.currentPhase.set(ResearchPhase.WRITING)
                state.currentStatus.set("Writing phase...")
                state.writtenReport.set(mockReport)
                state.currentPhase.set(ResearchPhase.COMPLETED)
                state.currentStatus.set("Research workflow completed!")
            }
            
            // Return the mock report wrapped in a trace
            AiPromptTrace(
                execInfo = AiExecInfo.durationSince(System.currentTimeMillis()),
                outputInfo = AiOutputInfo(listOf(mockReport))
            )
        }
    }

    /** Parses a plan text into a structured ResearchProjectPlan. */
    private fun parsePlanFromText(planText: String): ResearchProjectPlan {
        // Simple parsing - in a real implementation this would be more robust
        val lines = planText.lines()
        
        val objectives = extractSection(lines, "Research Objectives", "Key Questions")
        val questions = extractSection(lines, "Key Questions", "Research Methodology")
        val methodology = extractSection(lines, "Research Methodology", "Task Breakdown")
        
        val tasks = listOf(
            ResearchTask("1", "Literature Review", "Review existing research", "Planning", TaskPriority.HIGH, "2 hours"),
            ResearchTask("2", "Data Collection", "Gather relevant data", "Research", TaskPriority.HIGH, "4 hours"),
            ResearchTask("3", "Analysis", "Analyze findings", "Research", TaskPriority.MEDIUM, "3 hours"),
            ResearchTask("4", "Report Writing", "Draft final report", "Writing", TaskPriority.HIGH, "2 hours")
        )
        
        return ResearchProjectPlan(
            objectives = objectives,
            questions = questions,
            methodology = methodology,
            tasks = tasks,
            timeline = "Estimated 11 hours total",
            successCriteria = listOf("Comprehensive coverage", "Clear conclusions", "Proper citations")
        )
    }
    
    /** Extracts a section from text lines between two headers. */
    private fun extractSection(lines: List<String>, startHeader: String, endHeader: String): List<String> {
        val startIdx = lines.indexOfFirst { it.contains(startHeader, ignoreCase = true) }
        val endIdx = lines.indexOfFirst { it.contains(endHeader, ignoreCase = true) }
        
        if (startIdx == -1) return emptyList()
        
        val sectionLines = if (endIdx == -1) {
            lines.subList(startIdx + 1, lines.size)
        } else {
            lines.subList(startIdx + 1, endIdx)
        }
        
        return sectionLines.filter { it.trim().isNotEmpty() && !it.trim().startsWith("**") }
            .map { it.trim().removePrefix("- ").removePrefix("* ") }
            .filter { it.isNotEmpty() }
    }
    
    /** Parses report text into structured sections. */
    private fun parseReportSections(reportText: String): List<ReportSection> {
        val sections = mutableListOf<ReportSection>()
        val lines = reportText.lines()
        
        var currentSection: String? = null
        val currentContent = StringBuilder()
        
        for (line in lines) {
            when {
                line.startsWith("##") -> {
                    // Save previous section
                    if (currentSection != null) {
                        sections.add(ReportSection(currentSection, currentContent.toString().trim(), 2))
                    }
                    currentSection = line.removePrefix("##").trim()
                    currentContent.clear()
                }
                line.startsWith("#") -> {
                    // Save previous section
                    if (currentSection != null) {
                        sections.add(ReportSection(currentSection, currentContent.toString().trim(), 2))
                    }
                    currentSection = line.removePrefix("#").trim()
                    currentContent.clear()
                }
                else -> {
                    currentContent.appendLine(line)
                }
            }
        }
        
        // Add final section
        if (currentSection != null) {
            sections.add(ReportSection(currentSection, currentContent.toString().trim(), 1))
        }
        
        return sections
    }
}