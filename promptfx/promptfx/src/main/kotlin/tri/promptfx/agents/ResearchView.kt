/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.agents

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.stage.FileChooser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tornadofx.*
import tri.ai.research.ResearchOrchestrator
import tri.ai.research.ResearchSession
import tri.ai.research.WrittenReport
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxModels
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Plugin for the [ResearchView]. */
class ResearchPlugin : NavigableWorkspaceViewImpl<ResearchView>(
    "Agents", "Research", WorkspaceViewAffordance.INPUT_ONLY, ResearchView::class
)

/** A guided research view implementing an InfoPlanner → Research → Writing → Review agent workflow. */
class ResearchView : View("Research") {

    //region DEPENDENCIES

    private val controller: PromptFxController by inject()

    //endregion

    //region STATE

    /** Past research sessions shown in the history list. */
    private val sessions = observableListOf<ResearchSession>()

    /** The session currently in progress (or null if none active). */
    private val currentSession = SimpleObjectProperty<ResearchSession?>(null)

    /** Progress entries shown in the center panel. */
    private val progressEntries = observableListOf<ResearchProgressEntry>()

    /** Whether a workflow is currently running. */
    private val isRunning = SimpleBooleanProperty(false)

    /** Whether the "Continue" action is available (waiting for user to proceed). */
    private val canContinue = SimpleBooleanProperty(false)

    /** Whether a final report is ready for export. */
    private val canExport = SimpleBooleanProperty(false)

    /** Information request text typed by the user. */
    private val inputText = SimpleStringProperty("")

    /** The currently selected model. */
    private val modelId = controller.chatEngine

    /** Signal used to pause between agent steps until the user clicks "Continue". */
    private var continueSignal: CompletableDeferred<Unit>? = null

    /** Coroutine scope for the research workflow. */
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Reference to the artifacts TreeView, for updating items. */
    private lateinit var artifactsTree: TreeView<String>

    /** Reference to the center scroll pane, for auto-scrolling. */
    private lateinit var progressScrollPane: ScrollPane

    //endregion

    override val root = hbox {
        padding = insets(10.0)
        spacing = 10.0

        // LEFT PANEL – information request input + session history
        vbox(8.0) {
            prefWidth = 270.0
            minWidth = 220.0
            maxWidth = 320.0

            label("Research") {
                style { fontWeight = FontWeight.BOLD; fontSize = 18.px }
            }
            label("Enter an information request to start a guided research workflow.") {
                isWrapText = true
                style { fontSize = 12.px; textFill = Color.GRAY }
            }

            separator()

            label("Information Request") {
                style { fontWeight = FontWeight.BOLD }
            }
            textarea(inputText) {
                promptText = "Describe what you want to research…"
                isWrapText = true
                prefRowCount = 5
                vgrow = Priority.NEVER
                isDisable = isRunning.value
                isRunning.onChange { isDisable = it }
            }

            // Model selector
            hbox(6.0, Pos.CENTER_LEFT) {
                label("Model:")
                combobox(modelId, PromptFxModels.chatEngines()) {
                    hgrow = Priority.ALWAYS
                    maxWidth = Double.MAX_VALUE
                }
            }

            // Start / Stop button row
            hbox(6.0) {
                button("Start Research", FontAwesomeIconView(FontAwesomeIcon.PLAY)) {
                    hgrow = Priority.ALWAYS
                    maxWidth = Double.MAX_VALUE
                    style { fontSize = 13.px }
                    enableWhen(inputText.isNotEmpty.and(isRunning.not()))
                    action { startResearch() }
                }
            }

            separator()

            label("History") {
                style { fontWeight = FontWeight.BOLD }
            }
            listview(sessions) {
                vgrow = Priority.ALWAYS
                prefHeight = 0.0
                cellFormat { session ->
                    val preview = session.request.take(60).let { if (session.request.length > 60) "$it…" else it }
                    graphic = vbox(2.0) {
                        label(preview) {
                            isWrapText = true
                            style { fontWeight = FontWeight.BOLD; fontSize = 11.px }
                        }
                        val status = when {
                            session.report?.reviewNotes != null -> "✓ Complete"
                            session.report != null -> "Report drafted"
                            session.research != null -> "Research done"
                            session.plan != null -> "Plan ready"
                            else -> "In progress…"
                        }
                        label(status) {
                            style { fontSize = 10.px; textFill = Color.GRAY }
                        }
                    }
                }
                onUserSelect { session -> loadSession(session) }
            }
        }

        // CENTER PANEL – workflow progress messages
        borderpane {
            hgrow = Priority.ALWAYS

            top = vbox(4.0) {
                padding = insets(0.0, 0.0, 8.0, 0.0)
                hbox(10.0, Pos.CENTER_LEFT) {
                    label {
                        textProperty().bind(currentSession.stringBinding {
                            if (it != null) "Research: ${it.request.take(50)}${if (it.request.length > 50) "…" else ""}"
                            else "No active research session"
                        })
                        style { fontWeight = FontWeight.BOLD; fontSize = 14.px }
                    }
                    spacer()
                    // Blinking "thinking" indicator
                    label("● Working…") {
                        visibleWhen(isRunning)
                        managedWhen(isRunning)
                        style { textFill = Color.web("#1565C0"); fontStyle = FontPosture.ITALIC }
                    }
                }
                separator()
            }

            center = scrollpane(fitToWidth = true) {
                progressScrollPane = this
                vgrow = Priority.ALWAYS
                vbox(8.0) {
                    padding = insets(4.0)
                    bindChildren(progressEntries) { entry -> buildProgressBubble(entry) }
                    heightProperty().onChange { progressScrollPane.vvalue = 1.0 }
                }
            }

            bottom = vbox(6.0) {
                padding = insets(8.0, 0.0, 0.0, 0.0)
                separator()
                hbox(8.0, Pos.CENTER_LEFT) {
                    button("Continue", FontAwesomeIconView(FontAwesomeIcon.ARROW_RIGHT)) {
                        style { fontSize = 13.px }
                        visibleWhen(canContinue)
                        managedWhen(canContinue)
                        action { onContinueClicked() }
                    }
                    button("Export Report…", FontAwesomeIconView(FontAwesomeIcon.SAVE)) {
                        style { fontSize = 13.px }
                        visibleWhen(canExport)
                        managedWhen(canExport)
                        action { exportReport() }
                    }
                }
            }
        }

        // RIGHT PANEL – artifacts tree
        vbox(6.0) {
            prefWidth = 220.0
            minWidth = 180.0
            maxWidth = 260.0

            label("Research Artifacts") {
                style { fontWeight = FontWeight.BOLD }
            }
            artifactsTree = treeview {
                vgrow = Priority.ALWAYS
                root = TreeItem("Session")
                isShowRoot = true
                root.isExpanded = true
            }
        }
    }

    //region UI BUILDERS

    private fun buildProgressBubble(entry: ResearchProgressEntry) = when (entry.type) {
        ResearchStepType.USER_REQUEST -> hbox(8.0) {
            spacer()
            vbox(3.0) {
                alignment = Pos.CENTER_RIGHT
                label("You") {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.GRAY; fontSize = 11.px }
                    alignment = Pos.CENTER_RIGHT
                }
                label(entry.content) {
                    isWrapText = true
                    maxWidth = 420.0
                    padding = insets(8.0, 12.0)
                    style {
                        backgroundColor += Color.web("#d4e6ff")
                        backgroundRadius += box(10.px)
                        fontSize = 13.px
                    }
                }
            }
        }
        ResearchStepType.AGENT_STEP -> hbox(8.0) {
            vbox(3.0) {
                alignment = Pos.CENTER_LEFT
                label(entry.label) {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.web("#1565C0"); fontSize = 11.px }
                }
                label(entry.content) {
                    isWrapText = true
                    maxWidth = 420.0
                    padding = insets(8.0, 12.0)
                    style {
                        backgroundColor += Color.web("#e3f2fd")
                        backgroundRadius += box(10.px)
                        fontSize = 13.px
                    }
                }
            }
            spacer()
        }
        ResearchStepType.RESULT -> hbox(8.0) {
            vbox(3.0) {
                alignment = Pos.CENTER_LEFT
                label(entry.label) {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.web("#2E7D32"); fontSize = 11.px }
                }
                textarea(entry.content) {
                    isEditable = false
                    isWrapText = true
                    prefRowCount = 12
                    maxWidth = 460.0
                    style {
                        backgroundColor += Color.web("#e8f5e9")
                        backgroundRadius += box(6.px)
                        fontSize = 12.px
                    }
                }
            }
            spacer()
        }
        ResearchStepType.WAITING -> hbox(8.0) {
            vbox(3.0) {
                label(entry.label) {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.web("#7B5EA7"); fontSize = 11.px }
                }
                label(entry.content) {
                    isWrapText = true
                    maxWidth = 420.0
                    padding = insets(4.0, 8.0)
                    style {
                        textFill = Color.web("#555555")
                        backgroundColor += Color.web("#f5f0ff")
                        backgroundRadius += box(6.px)
                        fontSize = 12.px
                        fontStyle = FontPosture.ITALIC
                    }
                }
            }
            spacer()
        }
        ResearchStepType.ERROR -> hbox(8.0) {
            vbox(3.0) {
                label("Error") {
                    style { fontWeight = FontWeight.BOLD; textFill = Color.RED; fontSize = 11.px }
                }
                label(entry.content) {
                    isWrapText = true
                    maxWidth = 420.0
                    padding = insets(6.0, 10.0)
                    style {
                        textFill = Color.DARKRED
                        backgroundColor += Color.web("#fff0f0")
                        backgroundRadius += box(6.px)
                        fontSize = 12.px
                    }
                }
            }
            spacer()
        }
    }

    //endregion

    //region WORKFLOW CONTROL

    /** Starts a new research session with the current [inputText]. */
    private fun startResearch() {
        val request = inputText.value.trim()
        if (request.isBlank()) return

        // Reset state
        progressEntries.clear()
        canContinue.set(false)
        canExport.set(false)
        isRunning.set(true)

        val session = ResearchSession(request = request)
        currentSession.set(session)
        sessions.add(0, session)
        updateArtifactsTree(session)

        addProgress(ResearchStepType.USER_REQUEST, "Information Request", request)

        coroutineScope.launch {
            runResearchWorkflow(request)
        }
    }

    /** Loads a past session into the progress view. */
    private fun loadSession(session: ResearchSession) {
        progressEntries.clear()
        canContinue.set(false)
        canExport.set(session.report != null)
        currentSession.set(session)
        updateArtifactsTree(session)

        addProgress(ResearchStepType.USER_REQUEST, "Information Request", session.request)
        session.plan?.let { addProgress(ResearchStepType.RESULT, "✓ Research Plan", it) }
        session.research?.let { addProgress(ResearchStepType.RESULT, "✓ Research Findings", it) }
        session.report?.let { report ->
            addProgress(ResearchStepType.RESULT, "✓ Report Outline", report.outline)
            addProgress(ResearchStepType.RESULT, "✓ Final Report", report.content)
            report.reviewNotes?.let { addProgress(ResearchStepType.RESULT, "✓ Review Notes", it) }
        }
    }

    /** Runs the full multi-agent research workflow, pausing between each major stage. */
    private suspend fun runResearchWorkflow(request: String) {
        val chat = controller.chatEngine.value?.asTextChat()
            ?: run {
                addProgress(ResearchStepType.ERROR, "Error", "No chat model configured. Please select a model.")
                isRunning.set(false)
                return
            }
        val orchestrator = ResearchOrchestrator(chat)

        try {
            // --- STAGE 1: InfoPlanner Agent ---
            addProgress(ResearchStepType.AGENT_STEP, "InfoPlanner Agent", "Analyzing your request and generating a research plan…")
            val plan = orchestrator.generatePlan(request)
            updateCurrentSession { it.copy(plan = plan) }
            addProgress(ResearchStepType.RESULT, "Research Plan", plan)
            addProgress(
                ResearchStepType.WAITING, "Paused after Planning",
                "The research plan is ready. Click ▶ Continue to proceed to the Research phase."
            )
            waitForContinue()

            // --- STAGE 2: Research Agent ---
            addProgress(ResearchStepType.AGENT_STEP, "Research Agent", "Generating research questions and gathering findings…")
            val research = orchestrator.conductResearch(request, plan)
            updateCurrentSession { it.copy(research = research) }
            addProgress(ResearchStepType.RESULT, "Research Findings", research)
            addProgress(
                ResearchStepType.WAITING, "Paused after Research",
                "Research is complete. Click ▶ Continue to proceed to the Writing phase."
            )
            waitForContinue()

            // --- STAGE 3a: Writing Agent – Outline ---
            addProgress(ResearchStepType.AGENT_STEP, "Writing Agent", "Generating report outline…")
            val outline = orchestrator.generateOutline(request)
            addProgress(ResearchStepType.RESULT, "Report Outline", outline)

            // --- STAGE 3b: Writing Agent – Draft ---
            addProgress(ResearchStepType.AGENT_STEP, "Writing Agent", "Writing full report draft…")
            val draft = orchestrator.writeDraft(request, research, outline)
            updateCurrentSession { it.copy(report = WrittenReport(outline = outline, content = draft)) }
            addProgress(ResearchStepType.RESULT, "Report Draft", draft)
            addProgress(
                ResearchStepType.WAITING, "Paused after Writing",
                "Draft report is ready. Click ▶ Continue to proceed to the Review phase."
            )
            waitForContinue()

            // --- STAGE 4: Review Agent ---
            addProgress(ResearchStepType.AGENT_STEP, "Review Agent", "Evaluating the report for quality…")
            val reviewNotes = orchestrator.reviewReport(draft)
            updateCurrentSession {
                val updated = it.copy(report = it.report?.copy(reviewNotes = reviewNotes) ?: WrittenReport(outline, draft, reviewNotes))
                updated
            }
            addProgress(ResearchStepType.RESULT, "Review Notes", reviewNotes)
            addProgress(
                ResearchStepType.AGENT_STEP, "✓ Research Complete",
                "All agent phases are complete. You can export the final report using the Export button below."
            )

            canExport.set(true)
        } catch (e: Exception) {
            addProgress(ResearchStepType.ERROR, "Error", e.message ?: "An unexpected error occurred.")
        } finally {
            isRunning.set(false)
            canContinue.set(false)
        }

        updateArtifactsTree(currentSession.value)
    }

    /** Signals the workflow to proceed past the current pause point. */
    private fun onContinueClicked() {
        val signal = continueSignal ?: return
        continueSignal = null
        canContinue.set(false)
        signal.complete(Unit)
    }

    /** Suspends until the user clicks "Continue". */
    private suspend fun waitForContinue() {
        val deferred = CompletableDeferred<Unit>()
        continueSignal = deferred
        runLater { canContinue.set(true) }
        deferred.await()
    }

    //endregion

    //region EXPORT

    /** Exports the final report to a file chosen by the user. */
    private fun exportReport() {
        val session = currentSession.value ?: return
        val report = session.report ?: return
        val chooser = FileChooser().apply {
            title = "Save Research Report"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("Markdown Files", "*.md"),
                FileChooser.ExtensionFilter("Text Files", "*.txt"),
                FileChooser.ExtensionFilter("All Files", "*.*")
            )
            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            initialFileName = "research-report-$ts.md"
        }
        val file: File? = chooser.showSaveDialog(currentWindow)
        if (file != null) {
            val content = buildReportMarkdown(session, report)
            file.writeText(content)
            addProgress(
                ResearchStepType.AGENT_STEP, "Exported",
                "Report saved to: ${file.absolutePath}"
            )
        }
    }

    /** Formats the session into a complete Markdown report document. */
    private fun buildReportMarkdown(session: ResearchSession, report: WrittenReport): String = buildString {
        appendLine("# Research Report")
        appendLine()
        appendLine("**Information Request:** ${session.request}")
        appendLine()
        if (session.plan != null) {
            appendLine("## Research Plan")
            appendLine()
            appendLine(session.plan)
            appendLine()
        }
        if (session.research != null) {
            appendLine("## Research Findings")
            appendLine()
            appendLine(session.research)
            appendLine()
        }
        appendLine("## Report Outline")
        appendLine()
        appendLine(report.outline)
        appendLine()
        appendLine("## Report")
        appendLine()
        appendLine(report.content)
        if (report.reviewNotes != null) {
            appendLine()
            appendLine("## Review Notes")
            appendLine()
            appendLine(report.reviewNotes)
        }
    }

    //endregion

    //region HELPERS

    /** Adds a progress entry and scrolls to the bottom. */
    private fun addProgress(type: ResearchStepType, label: String, content: String) {
        runLater {
            progressEntries.add(ResearchProgressEntry(type, label, content))
        }
    }

    /** Updates the current session in-place and refreshes the history list and artifacts tree. */
    private fun updateCurrentSession(transform: (ResearchSession) -> ResearchSession) {
        val old = currentSession.value ?: return
        val updated = transform(old)
        currentSession.set(updated)
        val idx = sessions.indexOf(old)
        if (idx >= 0) sessions[idx] = updated
        runLater { updateArtifactsTree(updated) }
    }

    /** Rebuilds the artifacts [TreeView] to reflect the current session's state. */
    private fun updateArtifactsTree(session: ResearchSession?) {
        val root = TreeItem(if (session != null) "Session" else "No Session")
        root.isExpanded = true
        if (session != null) {
            root.children.add(TreeItem("📋 Request: ${session.request.take(30)}…"))
            if (session.plan != null) {
                root.children.add(TreeItem("🗂 Research Plan [✓]"))
            } else {
                root.children.add(TreeItem("🗂 Research Plan [ ]"))
            }
            if (session.research != null) {
                root.children.add(TreeItem("🔍 Research Findings [✓]"))
            } else {
                root.children.add(TreeItem("🔍 Research Findings [ ]"))
            }
            val outlineNode = if (session.report?.outline != null) {
                TreeItem("📝 Report Outline [✓]")
            } else {
                TreeItem("📝 Report Outline [ ]")
            }
            root.children.add(outlineNode)
            val reportNode = if (session.report?.content != null) {
                val node = TreeItem("📄 Report Draft [✓]")
                val reportLocal = session.report
                if (reportLocal != null && reportLocal.reviewNotes != null) {
                    node.children.add(TreeItem("🔎 Review Notes [✓]"))
                } else {
                    node.children.add(TreeItem("🔎 Review Notes [ ]"))
                }
                node.isExpanded = true
                node
            } else {
                TreeItem("📄 Report Draft [ ]")
            }
            root.children.add(reportNode)
        }
        artifactsTree.root = root
    }

    //endregion

}

/** An entry in the research workflow progress log. */
data class ResearchProgressEntry(
    val type: ResearchStepType,
    val label: String,
    val content: String
)

/** The type of a research progress entry, used to select the visual style. */
enum class ResearchStepType {
    /** The user's original information request. */
    USER_REQUEST,
    /** An agent is actively working on a step. */
    AGENT_STEP,
    /** A significant result produced by an agent. */
    RESULT,
    /** A pause point waiting for user confirmation to continue. */
    WAITING,
    /** An error occurred. */
    ERROR
}
