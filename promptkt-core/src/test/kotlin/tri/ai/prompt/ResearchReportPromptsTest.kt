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
package tri.ai.prompt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResearchReportPromptsTest {

    @Test
    fun testResearchQuestionsPromptExists() {
        val group = PromptGroupIO.readFromResource("research-report.yaml")
        assertEquals("research-report", group.groupId)
        assertEquals("research", group.defaults.category)
        
        val questionsPrompt = group.prompts.find { it.name == "questions" }
        assertNotNull(questionsPrompt)
        assertEquals("research-report/questions@1.0.0", questionsPrompt!!.id)
        assertEquals("Generate Research Questions", questionsPrompt.title)
        assertTrue(questionsPrompt.template!!.contains("Generate a comprehensive set of research questions"))
    }

    @Test
    fun testOutlinePromptExists() {
        val group = PromptGroupIO.readFromResource("research-report.yaml")
        val outlinePrompt = group.prompts.find { it.name == "outline" }
        assertNotNull(outlinePrompt)
        assertEquals("research-report/outline@1.0.0", outlinePrompt!!.id)
        assertEquals("Generate Report Outline", outlinePrompt.title)
        assertTrue(outlinePrompt.template!!.contains("Create a detailed"))
    }

    @Test
    fun testDraftPromptExists() {
        val group = PromptGroupIO.readFromResource("research-report.yaml")
        val draftPrompt = group.prompts.find { it.name == "draft" }
        assertNotNull(draftPrompt)
        assertEquals("research-report/draft@1.0.0", draftPrompt!!.id)
        assertEquals("Generate Draft Report", draftPrompt.title)
        assertTrue(draftPrompt.template!!.contains("Create a comprehensive draft report"))
        
        // Check that it has the required arguments
        val args = draftPrompt.args
        assertTrue(args.any { it.name == "topic" && it.required })
        assertTrue(args.any { it.name == "research" && it.required })
        assertTrue(args.any { it.name == "outline" && it.required })
        assertTrue(args.any { it.name == "tone" && !it.required })
    }

    @Test
    fun testReviewEditPromptExists() {
        val group = PromptGroupIO.readFromResource("research-report.yaml")
        val reviewPrompt = group.prompts.find { it.name == "review-edit" }
        assertNotNull(reviewPrompt)
        assertEquals("research-report/review-edit@1.0.0", reviewPrompt!!.id)
        assertEquals("Review and Edit Report Section", reviewPrompt.title)
        assertTrue(reviewPrompt.template!!.contains("Review and edit the following report section"))
    }
}