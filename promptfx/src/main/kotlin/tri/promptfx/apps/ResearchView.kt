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
package tri.promptfx.apps

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.instructTextPlan
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.AiPlanTaskView
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [ResearchView]. */
class ResearchViewPlugin : NavigableWorkspaceViewImpl<ResearchView>("Research", "Research View", WorkspaceViewAffordance.INPUT_ONLY, ResearchView::class)

/** 
 * View with guided research and report writing process.
 * Implements InfoRequest -> Plan -> Research -> Write -> Review workflow.
 */
class ResearchView: AiPlanTaskView("Research View",
    "Enter an information request to begin a guided research and report writing process.") {

    companion object {
        private const val PROMPT_PREFIX_PLAN = "research-plan"
        private const val PROMPT_PREFIX_QUESTIONS = "research-questions" 
        private const val PROMPT_PREFIX_OUTLINE = "research-outline"
        private const val PROMPT_PREFIX_WRITE = "research-write"
        private const val PROMPT_PREFIX_REVIEW = "research-review"
    }

    // Data properties
    private val informationRequest = SimpleStringProperty("")
    private val currentPhase = SimpleStringProperty("Information Request")
    private val researchPlan = SimpleStringProperty("")
    private val researchQuestions = SimpleStringProperty("")
    private val reportOutline = SimpleStringProperty("")
    private val draftReport = SimpleStringProperty("")
    private val finalReport = SimpleStringProperty("")
    
    // UI properties
    private val selectedPhase = SimpleStringProperty("")
    private val phaseResults = FXCollections.observableArrayList<ResearchPhaseResult>()
    private val treeRoot = TreeItem("Research Project")
    private val hasResults = SimpleBooleanProperty(false)
    
    // Prompt models
    private val planPrompt = PromptSelectionModel(PROMPT_PREFIX_PLAN)
    private val questionsPrompt = PromptSelectionModel(PROMPT_PREFIX_QUESTIONS)
    private val outlinePrompt = PromptSelectionModel(PROMPT_PREFIX_OUTLINE)
    private val writePrompt = PromptSelectionModel(PROMPT_PREFIX_WRITE)
    private val reviewPrompt = PromptSelectionModel(PROMPT_PREFIX_REVIEW)

    init {
        setupUI()
    }

    private fun setupUI() {
        // Left side: Input and workflow controls
        hbox {
            vbox {
                hboxConstraints { hGrow = Priority.SOMETIMES }
                prefWidth = 400.0
                
                // Information Request input
                label("Information Request:")
                textarea(informationRequest) {
                    promptText = "Enter your information request here. For example: 'What are the current trends in renewable energy adoption and their economic impacts?'"
                    isWrapText = true
                    prefRowCount = 4
                }
                
                // Workflow control buttons
                hbox {
                    spacing = 10.0
                    button("Start Research") {
                        action { startResearchWorkflow() }
                        disableProperty().bind(informationRequest.isEmpty)
                    }
                    button("Continue to Next Phase") {
                        action { continueToNextPhase() }
                        disableProperty().bind(hasResults.not())
                    }
                    button("Export Report") {
                        action { exportReport() }
                        disableProperty().bind(finalReport.isEmpty)
                    }
                }
                
                // Current phase display
                label("Current Phase:")
                label(currentPhase) {
                    style {
                        fontSize = 14.px
                        fontWeight = javafx.scene.text.FontWeight.BOLD
                    }
                }
                
                // Phase results list
                label("Progress:")
                listview(phaseResults) {
                    prefHeight = 200.0
                    cellFormat { item ->
                        text = "${item.phase}: ${item.status}"
                        style {
                            if (item.status == "Complete") {
                                textFill = javafx.scene.paint.Color.GREEN
                            } else if (item.status == "In Progress") {
                                textFill = javafx.scene.paint.Color.ORANGE  
                            }
                        }
                    }
                }
                
                // Prompt configuration
                parameters("Prompt Templates") {
                    promptfield("Planning", planPrompt, AiPromptLibrary.withPrefix(PROMPT_PREFIX_PLAN), workspace)
                    promptfield("Questions", questionsPrompt, AiPromptLibrary.withPrefix(PROMPT_PREFIX_QUESTIONS), workspace)
                    promptfield("Outline", outlinePrompt, AiPromptLibrary.withPrefix(PROMPT_PREFIX_OUTLINE), workspace)  
                    promptfield("Writing", writePrompt, AiPromptLibrary.withPrefix(PROMPT_PREFIX_WRITE), workspace)
                    promptfield("Review", reviewPrompt, AiPromptLibrary.withPrefix(PROMPT_PREFIX_REVIEW), workspace)
                }
                
                addDefaultTextCompletionParameters(common)
            }
            
            // Right side: Results and tree view 
            vbox {
                hboxConstraints { hGrow = Priority.ALWAYS }
                
                // Tree view showing project structure
                label("Project Structure:")
                TreeView(treeRoot).apply {
                    prefHeight = 300.0
                    showRootProperty().set(true)
                }
                
                // Results area
                label("Results:")
            }
        }
    }
    
    private fun startResearchWorkflow() {
        phaseResults.clear()
        treeRoot.children.clear()
        hasResults.set(false)
        
        // Initialize workflow phases
        phaseResults.addAll(
            ResearchPhaseResult("1. Research Planning", "Pending"),
            ResearchPhaseResult("2. Question Generation", "Pending"),  
            ResearchPhaseResult("3. Outline Creation", "Pending"),
            ResearchPhaseResult("4. Content Writing", "Pending"),
            ResearchPhaseResult("5. Review & Finalization", "Pending")
        )
        
        currentPhase.set("Research Planning")
        runPlanningPhase()
    }
    
    private fun runPlanningPhase() {
        phaseResults[0].status = "In Progress"
        runTask {
            val result = AiPipelineExecutor.execute(
                completionEngine.instructTextPlan(
                    planPrompt.prompt.value,
                    instruct = "",
                    userText = informationRequest.get(),
                    tokenLimit = common.maxTokens.value!!,
                    temp = common.temp.value,
                    numResponses = common.numResponses.value
                ).plan(),
                progress
            )
            
            runLater {
                researchPlan.set(result.finalResult?.toString() ?: "")
                phaseResults[0].status = "Complete"
                hasResults.set(true)
                addTreeItem("Research Plan", researchPlan.get())
                addTrace(result.finalResult)
            }
            
            result
        }
    }
    
    private fun continueToNextPhase() {
        val currentPhaseIndex = phaseResults.indexOfFirst { it.status == "Complete" }
        when (currentPhaseIndex) {
            0 -> runQuestionsPhase()
            1 -> runOutlinePhase() 
            2 -> runWritingPhase()
            3 -> runReviewPhase()
            4 -> {
                currentPhase.set("Complete")
                information("Research workflow complete!")
            }
        }
    }
    
    private fun runQuestionsPhase() {
        phaseResults[1].status = "In Progress"
        currentPhase.set("Question Generation")
        
        runTask {
            val result = AiPipelineExecutor.execute(
                completionEngine.instructTextPlan(
                    questionsPrompt.prompt.value,
                    instruct = "",
                    userText = informationRequest.get(),
                    tokenLimit = common.maxTokens.value!!,
                    temp = common.temp.value,
                    numResponses = common.numResponses.value
                ).plan(),
                progress
            )
            
            runLater {
                researchQuestions.set(result.finalResult?.toString() ?: "")
                phaseResults[1].status = "Complete"
                addTreeItem("Research Questions", researchQuestions.get())
                addTrace(result.finalResult)
            }
            
            result
        }
    }
    
    private fun runOutlinePhase() {
        phaseResults[2].status = "In Progress"
        currentPhase.set("Outline Creation")
        
        val context = "Research Plan:\n${researchPlan.get()}\n\nResearch Questions:\n${researchQuestions.get()}"
        
        runTask {
            val result = AiPipelineExecutor.execute(
                completionEngine.instructTextPlan(
                    outlinePrompt.prompt.value,
                    instruct = "",
                    userText = context,
                    tokenLimit = common.maxTokens.value!!,
                    temp = common.temp.value,
                    numResponses = common.numResponses.value
                ).plan(),
                progress
            )
            
            runLater {
                reportOutline.set(result.finalResult?.toString() ?: "")
                phaseResults[2].status = "Complete"
                addTreeItem("Report Outline", reportOutline.get())
                addTrace(result.finalResult)
            }
            
            result
        }
    }
    
    private fun runWritingPhase() {
        phaseResults[3].status = "In Progress"
        currentPhase.set("Content Writing")
        
        val context = "Research Plan:\n${researchPlan.get()}\n\nOutline:\n${reportOutline.get()}"
        
        runTask {
            val result = AiPipelineExecutor.execute(
                completionEngine.instructTextPlan(
                    writePrompt.prompt.value,
                    instruct = "Write a complete report based on the outline",
                    userText = context,
                    tokenLimit = common.maxTokens.value!!,
                    temp = common.temp.value,
                    numResponses = common.numResponses.value
                ).plan(),
                progress
            )
            
            runLater {
                draftReport.set(result.finalResult?.toString() ?: "")
                phaseResults[3].status = "Complete"
                addTreeItem("Draft Report", draftReport.get())
                addTrace(result.finalResult)
            }
            
            result
        }
    }
    
    private fun runReviewPhase() {
        phaseResults[4].status = "In Progress"
        currentPhase.set("Review & Finalization")
        
        runTask {
            val result = AiPipelineExecutor.execute(
                completionEngine.instructTextPlan(
                    reviewPrompt.prompt.value,
                    instruct = "Review for clarity, completeness, and accuracy",
                    userText = draftReport.get(),
                    tokenLimit = common.maxTokens.value!!,
                    temp = common.temp.value,
                    numResponses = common.numResponses.value
                ).plan(),
                progress
            )
            
            runLater {
                finalReport.set(result.finalResult?.toString() ?: "")
                phaseResults[4].status = "Complete"
                addTreeItem("Final Report", finalReport.get())
                addTrace(result.finalResult)
            }
            
            result
        }
    }
    
    private fun exportReport() {
        val reportContent = """
            # Research Report
            
            ## Information Request
            ${informationRequest.get()}
            
            ## Research Plan
            ${researchPlan.get()}
            
            ## Research Questions
            ${researchQuestions.get()}
            
            ## Report Outline
            ${reportOutline.get()}
            
            ## Final Report
            ${finalReport.get()}
            
            ---
            Generated on: ${java.time.LocalDateTime.now()}
        """.trimIndent()
        
        try {
            val file = java.io.File("research-report-${System.currentTimeMillis()}.md")
            file.writeText(reportContent)
            information("Report exported to: ${file.absolutePath}")
        } catch (e: Exception) {
            error("Failed to export report: ${e.message}")
        }
    }
    
    private fun addTreeItem(title: String, content: String) {
        val item = TreeItem("$title (${content.take(50)}...)")
        treeRoot.children.add(item)
        treeRoot.isExpanded = true
    }

    override fun plan() = completionEngine.instructTextPlan(
        planPrompt.prompt.value,
        instruct = "",
        userText = informationRequest.get(),
        tokenLimit = common.maxTokens.value!!,
        temp = common.temp.value,
        numResponses = common.numResponses.value
    )

    data class ResearchPhaseResult(
        val phase: String,
        var status: String
    )
}