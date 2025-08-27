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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ResearchViewTest {

    @Test
    fun `test InfoRequest creation`() {
        val request = InfoRequest("Test research question")
        assertEquals("Test research question", request.request)
        assertTrue(request.timestamp > 0)
        assertEquals("Test research question", request.toString())
    }
    
    @Test
    fun `test ResearchProjectPlan summary`() {
        val plan = ResearchProjectPlan(
            objectives = listOf("Obj1", "Obj2"),
            questions = listOf("Q1", "Q2", "Q3"),
            methodology = listOf("Method1"),
            tasks = listOf(
                ResearchTask("1", "Task1", "Desc1", "Phase1", TaskPriority.HIGH, "1h"),
                ResearchTask("2", "Task2", "Desc2", "Phase2", TaskPriority.LOW, "2h")
            ),
            timeline = "3 hours",
            successCriteria = listOf("Criteria1")
        )
        
        assertEquals("Plan with 2 objectives, 3 questions, 2 tasks", plan.summary())
    }
    
    @Test
    fun `test ResearchPack findings count`() {
        val pack = ResearchPack(
            findings = listOf(
                ResearchFinding("Topic1", "Content1", "Source1"),
                ResearchFinding("Topic2", "Content2", "Source2"),
                ResearchFinding("Topic3", "Content3", "Source3")
            ),
            sources = listOf("Source1", "Source2", "Source3"),
            summary = "Test summary"
        )
        
        assertEquals(3, pack.totalFindings())
    }
    
    @Test
    fun `test WrittenReport word count`() {
        val report = WrittenReport(
            title = "Test Report",
            sections = listOf(
                ReportSection("Introduction", "This is the intro", 1),
                ReportSection("Body", "This is the body content", 1)
            ),
            outline = "1. Introduction\n2. Body",
            fullText = "This is a test report with multiple words for counting",
            citations = listOf("Source1")
        )
        
        assertEquals(10, report.wordCount())
    }
    
    @Test
    fun `test ResearchWorkflowState initial state`() {
        val state = ResearchWorkflowState()
        
        assertEquals(ResearchPhase.PLANNING, state.currentPhase.value)
        assertNull(state.infoRequest.value)
        assertNull(state.researchPlan.value)
        assertNull(state.researchPack.value)
        assertNull(state.writtenReport.value)
        assertEquals("Ready to begin research", state.currentStatus.value)
        assertEquals(false, state.isProcessing.value)
    }
    
    @Test
    fun `test ResearchViewPlugin configuration`() {
        val plugin = ResearchViewPlugin()
        
        assertEquals("Research", plugin.category)
        assertEquals("Research View", plugin.name)
        assertEquals(true, plugin.affordances.acceptsInput)
        assertEquals(false, plugin.affordances.acceptsCollection)
        assertEquals(false, plugin.affordances.producesOutput)
    }
}