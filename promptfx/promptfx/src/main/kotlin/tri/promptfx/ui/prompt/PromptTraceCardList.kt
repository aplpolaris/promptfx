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
package tri.promptfx.ui.prompt

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TextInputDialog
import javafx.scene.layout.Priority
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.trace.AiModelInfo.Companion.CHUNKER_MAX_CHUNK_SIZE
import tri.ai.prompt.trace.AiModelInfo.Companion.EMBEDDING_MODEL
import tri.ai.prompt.trace.AiModelInfo.Companion.MAX_TOKENS
import tri.ai.prompt.trace.AiModelInfo.Companion.TEMPERATURE
import tri.ai.prompt.trace.AiTaskTrace
import tri.ai.prompt.trace.writeTraceDatabase
import tri.ai.prompt.trace.writeTraces
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TRACE
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_CSV
import tri.promptfx.PromptFxConfig.Companion.FF_JSON
import tri.promptfx.PromptFxConfig.Companion.FF_YAML
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.buildsendresultmenu
import tri.promptfx.promptFxFileChooser
import tri.promptfx.prompts.PromptTraceFilter
import tri.promptfx.ui.LOCATE_IN_PROMPT_HISTORY
import tri.promptfx.ui.SEND_TO_PROMPT_TEMPLATE
import tri.util.ui.checklistmenu
import tri.util.ui.graphic
import java.io.File
import kotlin.text.isNotBlank

/** UI for a list of [AiTaskTrace]s. */
class PromptTraceCardList: Fragment() {

    private val isRemovable: Boolean by param(false)
    private val isShowFilter: Boolean by param(false)
    private val toolbarLabel: String by param("Results:")
    val traces: ObservableList<AiTaskTrace> by param()

    private val controller: PromptFxController by inject()
    private val isGlobalHistoryView = controller.traceHistory.prompts === traces
    private val traceFilter: PromptTraceFilter = find<PromptTraceFilter>()
    private val filteredTraces = observableListOf<AiTaskTrace>()
    private lateinit var traceSelectionModel: MultipleSelectionModel<AiTaskTrace>
    val selectedPrompt = SimpleObjectProperty<AiTaskTrace>()

    init {
        traces.onChange {
            traceFilter.updateFilterOptions(it.list)
            refilter()
        }
        traceFilter.filter.onChange { refilter() }
        traceFilter.updateFilterOptions(traces)
        refilter()
    }

    override val root = vbox {
        vgrow = Priority.ALWAYS
        val header = toolbar {
            text(toolbarLabel)
            spacer()
        }
        if (isShowFilter) {
            toolbar {
                label("Filter by:")
                checklistmenu("view", traceFilter.viewFilters) { refilter() }
                checklistmenu("model", traceFilter.modelFilters) { refilter() }
                checklistmenu("status", traceFilter.statusFilters) { refilter() }
                checklistmenu("type", traceFilter.typeFilters) { refilter() }
            }
        }
        val list = listview(filteredTraces) {
            vgrow = Priority.ALWAYS
            traceSelectionModel = selectionModel
            selectedPrompt.bind(selectionModel.selectedItemProperty())
            cellFormat {
                graphic = PromptTraceCard().apply { setTrace(it) }.root
            }
            // add context menu
            lazyContextmenu {
                item("Trace details...") {
                    enableWhen(selectionModel.selectedItemProperty().isNotNull)
                    action {
                        val selected = selectionModel.selectedItem
                        if (selected != null)
                            find<PromptTraceDetailsUi>().apply {
                                setTrace(selected)
                                openModal()
                            }
                    }
                }
                item(SEND_TO_PROMPT_TEMPLATE, graphic = FontAwesomeIcon.SEND.graphic) {
                    enableWhen(selectionModel.selectedItemProperty().booleanBinding {
                        it?.input?.prompt?.isNotBlank() == true
                    })
                    action {
                        val selected = selectionModel.selectedItem
                        if (selected != null)
                            find<PromptFxWorkspace>().launchTemplateView(selected)
                    }
                }
                if (!isGlobalHistoryView) {
                    item(LOCATE_IN_PROMPT_HISTORY, graphic = FontAwesomeIcon.SEARCH.graphic) {
                        enableWhen(selectionModel.selectedItemProperty().booleanBinding {
                            it?.input?.prompt?.isNotBlank() == true
                        })
                        action {
                            val selected = selectionModel.selectedItem
                            if (selected != null)
                                find<PromptFxWorkspace>().launchHistoryView(selected)
                        }
                    }
                }
                buildsendresultmenu("result", selectedPrompt.value?.firstValue?.toString(), find<PromptFxWorkspace>())
            }
        }
        with (header) {
            button("", FontAwesomeIcon.SEND.graphic) {
                tooltip("Try out the selected prompt and inputs in the Prompt Template view.")
                enableWhen(list.selectionModel.selectedItemProperty().isNotNull)
                action {
                    val selected = list.selectedItem
                    if (selected != null)
                        find<PromptFxWorkspace>().launchTemplateView(selected)
                }
            }
            if (isRemovable) {
                button("", FontAwesomeIcon.REMOVE.graphic) {
                    tooltip("Remove selected traces.")
                    enableWhen(list.selectionModel.selectedItemProperty().isNotNull)
                    action {
                        confirm(
                            "Remove traces?",
                            "Are you sure you want to remove selected prompt traces?",
                            owner = currentWindow
                        ) {
                            val selection = list.selectionModel.selectedItems.toList()
                            traces.removeAll(selection)
                            list.selectionModel.clearSelection()
                        }
                    }
                }
            }
            menubutton("", FontAwesomeIcon.DOWNLOAD.graphic) {
                tooltip("Export traces with details to a file.")
                enableWhen(traces.sizeProperty.greaterThan(0))
                item("Export as JSON/YAML List...") {
                    action {
                        exportTraceList(filteredTraces.toList())
                    }
                }
                item("Export as JSON/YAML Database...") {
                    action {
                        exportTraceDatabase(filteredTraces.toList())
                    }
                }
                item("Export as CSV...") {
                    action {
                        exportTraceCsv(filteredTraces.toList())
                    }
                }
            }
            if (isRemovable) {
                button("", FontAwesomeIcon.TRASH.graphic) {
                    enableWhen(traces.sizeProperty.greaterThan(0))
                    action {
                        confirm("Clear all traces?", "Are you sure you want to clear all traces?", owner = currentWindow) {
                            traces.clear()
                            list.selectionModel.clearSelection()
                        }
                    }
                }
            }

            // settings enabled for global history only
            if (isGlobalHistoryView) {
                button("", FontAwesomeIcon.GEARS.graphic) {
                    tooltip("Adjust trace history settings.")
                    action {
                        TextInputDialog(controller.traceHistory.maxHistorySize.value.toString()).apply {
                            initOwner(currentWindow)
                            title = "Adjust Trace History Settings"
                            headerText = "Enter max # of traces to keep in history."
                            contentText = "Max Entries:"
                            showAndWait().ifPresent {
                                it.toIntOrNull()?.let { controller.traceHistory.maxHistorySize.set(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refilter() {
        if (isShowFilter) {
            val filter = traceFilter.filter.value
            filteredTraces.setAll(traces.toList().filter(filter))
        } else {
            filteredTraces.setAll(traces.toList())
        }
    }

    fun selectTrace(trace: AiTaskTrace) {
        traceSelectionModel.select(trace)
    }

}

/** Exports the given list of prompt traces to a JSON file. */
fun UIComponent.exportTraceList(traces: List<AiTaskTrace>) {
    promptFxFileChooser(
        dirKey = DIR_KEY_TRACE,
        title = "Export Prompt Traces as JSON/YAML List",
        filters = arrayOf(FF_JSON, FF_YAML, FF_ALL),
        mode = FileChooserMode.Save
    ) { file ->
        file.first { writeTraces(traces, it) }
    }
}

/** Exports the given list of prompt traces as a [tri.ai.prompt.trace.AiTaskTraceDatabase] to a user-selected file. */
fun UIComponent.exportTraceDatabase(traces: List<AiTaskTrace>) {
    promptFxFileChooser(
        dirKey = DIR_KEY_TRACE,
        title = "Export Prompt Traces as JSON/YAML Database",
        filters = arrayOf(FF_JSON, FF_YAML, FF_ALL),
        mode = FileChooserMode.Save
    ) { file ->
        file.first { writeTraceDatabase(traces, it) }
    }
}

/** Exports the given list of prompt traces as a CSV file. */
fun UIComponent.exportTraceCsv(traces: List<AiTaskTrace>) {
    promptFxFileChooser(
        dirKey = DIR_KEY_TRACE,
        title = "Export Prompt Traces as CSV",
        filters = arrayOf(FF_CSV, FF_ALL),
        mode = FileChooserMode.Save
    ) { file ->
        file.first { writeTraceListCsv(traces, it) }
    }
}

/** Utility to run on first file in a background thread. */
private fun List<File>.first(op: (File) -> Unit) {
    firstOrNull()?.let {
        runAsync {
            runBlocking {
                op(it)
            }
        }
    }
}

/** Utility for writing a Q&A row to a CSV file. */
@JsonPropertyOrder(
    "ragEngine",
    "embeddingModel",
    "chunkSize",
    "chatModel",
    "temperature",
    "responseTokens",
    "question",
    "answer",
    "answerIndex",
    "error"
)

data class DocQaCsvRow(
    @JsonProperty("RAG Engine")
    val ragEngine: String,
    @JsonProperty("Embedding Model")
    val embeddingModel: Any?,
    @JsonProperty("Chunk Size")
    val chunkSize: Any?,
    @JsonProperty("Chat Model")
    val chatModel: String,
    @JsonProperty("Temperature")
    val temperature: Any?,
    @JsonProperty("Response Tokens")
    val responseTokens: Any?,
    @JsonProperty("Question")
    val question: Any?,
    @JsonProperty("Answer")
    val answer: String,
    @JsonProperty("Answer Index")
    val answerIndex: Int,
    @JsonProperty("Error")
    val error: String?
)

/** Writes the given [tri.ai.prompt.trace.AiTaskTraceDatabase] to the specified CSV file. */
fun writeTraceListCsv(traces: List<AiTaskTrace>, file: File) {
    val mapper = CsvMapper().apply {
        registerKotlinModule()
    }
    val schema = mapper.schemaFor(DocQaCsvRow::class.java).withHeader()
    val rows = traces.flatMap { t ->
        t.output?.outputs?.mapIndexed { i, output ->
            DocQaCsvRow("PromptFx",
                t.env?.modelParams?.get(EMBEDDING_MODEL),
                t.env?.modelParams?.get(CHUNKER_MAX_CHUNK_SIZE),
                t.env?.modelId ?: "unknown",
                t.env?.modelParams?.get(TEMPERATURE),
                t.env?.modelParams?.get(MAX_TOKENS),
                t.input?.params?.get(PromptTemplate.INSTRUCT),
                output.toString(),
                i+1,
                null
            )
        } ?:
        listOf(DocQaCsvRow("PromptFx",
            "unknown",
            "0",
            t.env?.modelId ?: "unknown",
            t.env?.modelParams?.get(TEMPERATURE),
            t.env?.modelParams?.get(MAX_TOKENS),
            t.input?.params?.get(PromptTemplate.INPUT),
            "",
            0,
            "No output"
        ))
    }
    mapper.writer(schema).writeValue(file, rows)
}
