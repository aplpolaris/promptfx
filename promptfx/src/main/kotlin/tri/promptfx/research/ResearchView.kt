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

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.util.Callback
import tornadofx.*
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiPlanner
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.AiPlanTaskView
import tri.util.info
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import java.io.File

/** Plugin for the [ResearchView]. */
class ResearchViewPlugin : NavigableWorkspaceViewImpl<ResearchView>("Research", "Research View", WorkspaceViewAffordance.INPUT_ONLY, ResearchView::class)

/** Main research view that provides a guided process for research and report writing. */
class ResearchView : AiPlanTaskView(
    "Research View",
    "Enter an information request to begin guided research and report writing process."
) {

    private val workflowState = ResearchWorkflowState()
    private val planner = ResearchPlannerFx(workflowState)
    
    // UI Properties
    private val infoRequestText = SimpleStringProperty("")
    private val isWorkflowRunning = SimpleBooleanProperty(false)
    private val canProceedToNext = SimpleBooleanProperty(false)
    
    // UI Components
    private lateinit var progressTree: TreeView<String>
    private lateinit var statusLabel: Label
    private lateinit var proceedButton: Button
    private lateinit var exportButton: Button

    init {
        setupWorkflowStateBindings()
        setupUI()
    }
    
    private fun setupUI() {
        // Setup input pane with research request input
        inputPane.apply {
            label("Information Request") {
                style = "-fx-font-weight: bold; -fx-font-size: 14px;"
            }
            
            textarea(infoRequestText) {
                prefRowCount = 4
                isWrapText = true
                promptText = "Enter your research request or question here..."
            }
            
            hbox {
                spacing = 10.0
                button("Start Research") {
                    enableWhen { infoRequestText.isNotEmpty and !isWorkflowRunning }
                    action { startResearchWorkflow() }
                }
                button("Clear") {
                    enableWhen { !isWorkflowRunning }
                    action { infoRequestText.set("") }
                }
            }
        }
        
        // Setup output pane with progress and results  
        outputPane.clear()
        outputPane.apply {
            // Status and Controls
            hbox {
                spacing = 10.0
                
                statusLabel = label(workflowState.currentStatus) {
                    style = "-fx-font-weight: bold;"
                    hgrow = Priority.ALWAYS
                }
                
                proceedButton = button("Continue") {
                    enableWhen { canProceedToNext and !isWorkflowRunning }
                    action { proceedToNextStep() }
                }
                
                exportButton = button("Export Report") {
                    enableWhen { workflowState.currentPhase.isEqualTo(ResearchPhase.COMPLETED) }
                    action { exportReport() }
                }
            }
            
            separator()
            
            // Progress Tree
            label("Research Progress") {
                style = "-fx-font-weight: bold; -fx-font-size: 14px;"
            }
            
            progressTree = treeview<String> {
                prefHeight = 200.0
                root = TreeItem("Research Workflow")
                root.isExpanded = true
                
                // Initialize tree structure  
                val planningItem = TreeItem("1. Planning Phase")
                val researchItem = TreeItem("2. Research Phase") 
                val writingItem = TreeItem("3. Writing Phase")
                val reviewItem = TreeItem("4. Review Phase")
                
                root.children.addAll(planningItem, researchItem, writingItem, reviewItem)
            }
            
            add(formattedResultArea)
        }
    }

    private fun setupWorkflowStateBindings() {
        // Bind status label
        statusLabel.textProperty().bind(workflowState.currentStatus)
        
        // Update progress tree based on current phase
        workflowState.currentPhase.onChange { phase ->
            updateProgressTree(phase ?: ResearchPhase.PLANNING)
        }
    }

    private fun updateProgressTree(phase: ResearchPhase) {
        // Update tree visual state based on current phase
        val root = progressTree.root
        if (root != null && root.children.size >= 4) {
            when (phase) {
                ResearchPhase.PLANNING -> {
                    root.children[0].value = "1. Planning Phase ✓"
                    root.children[1].value = "2. Research Phase"
                    root.children[2].value = "3. Writing Phase"  
                    root.children[3].value = "4. Review Phase"
                }
                ResearchPhase.RESEARCH -> {
                    root.children[0].value = "1. Planning Phase ✓"
                    root.children[1].value = "2. Research Phase ⏳"
                    root.children[2].value = "3. Writing Phase"
                    root.children[3].value = "4. Review Phase"
                }
                ResearchPhase.WRITING -> {
                    root.children[0].value = "1. Planning Phase ✓"
                    root.children[1].value = "2. Research Phase ✓"
                    root.children[2].value = "3. Writing Phase ⏳"
                    root.children[3].value = "4. Review Phase"
                }
                ResearchPhase.REVIEW -> {
                    root.children[0].value = "1. Planning Phase ✓"
                    root.children[1].value = "2. Research Phase ✓"
                    root.children[2].value = "3. Writing Phase ✓"
                    root.children[3].value = "4. Review Phase ⏳"
                }
                ResearchPhase.COMPLETED -> {
                    root.children[0].value = "1. Planning Phase ✓"
                    root.children[1].value = "2. Research Phase ✓"
                    root.children[2].value = "3. Writing Phase ✓"
                    root.children[3].value = "4. Review Phase ✓"
                }
            }
        }
    }

    private fun startResearchWorkflow() {
        val request = infoRequestText.value.trim()
        if (request.isEmpty()) {
            error("Please enter a research request")
            return
        }
        
        val infoRequest = InfoRequest(request)
        workflowState.infoRequest.set(infoRequest)
        
        // Clear previous results and start the workflow
        isWorkflowRunning.set(true)
        runTask()
    }

    private fun proceedToNextStep() {
        // In a full implementation, this would control step-by-step execution
        info<ResearchView>("User chose to proceed to next step")
        canProceedToNext.set(false)
    }

    private fun pauseWorkflow() {
        // In a full implementation, this would pause execution
        info<ResearchView>("User requested to pause workflow") 
        isWorkflowRunning.set(false)
    }

    private fun exportReport() {
        val report = workflowState.writtenReport.value
        if (report == null) {
            error("No report available to export")
            return
        }
        
        val fileChooser = FileChooser()
        fileChooser.title = "Save Research Report"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Text Files", "*.txt"),
            FileChooser.ExtensionFilter("Markdown Files", "*.md"),
            FileChooser.ExtensionFilter("All Files", "*.*")
        )
        fileChooser.initialFileName = "research-report.txt"
        
        val file = fileChooser.showSaveDialog(currentWindow)
        if (file != null) {
            try {
                file.writeText(report.fullText)
                information("Report exported successfully to: ${file.absolutePath}")
            } catch (e: Exception) {
                error("Error exporting report: ${e.message}")
            }
        }
    }

    override fun plan(): AiPlanner {
        val infoRequest = workflowState.infoRequest.value 
            ?: throw IllegalStateException("No information request available")
            
        return object : AiPlanner {
            override fun plan() = planner.createResearchWorkflow(
                infoRequest,
                controller.completionEngine.value,
                common.maxTokens.value,
                common.temp.value
            ).plan
        }
    }

    override fun addTrace(trace: AiPromptTraceSupport<*>) {
        super.addTrace(trace)
        isWorkflowRunning.set(false)
        if (trace.firstValue is WrittenReport) {
            info<ResearchView>("Research workflow completed successfully")
        }
    }
}