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
package tri.promptfx.mcp

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import kotlinx.coroutines.*
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxMcpController
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.warning

/** Plugin for the [McpPromptView]. */
class McpPromptPlugin : NavigableWorkspaceViewImpl<McpPromptView>("MCP", "MCP Prompts", type = McpPromptView::class)

/** View and try out MCP server prompts. */
class McpPromptView : AiTaskView("MCP Prompts", "View and test prompts for configured MCP servers (prompts/list, prompts/get).") {

    private val mcpController: PromptFxMcpController by inject()

    private val promptEntries = observableListOf<McpPromptWithServer>()
    private var promptNameFilter: (String) -> Boolean = { true }
    private val filteredPromptEntries = observableListOf<McpPromptWithServer>()
    private val promptSelection = SimpleObjectProperty<McpPromptWithServer>()
    private lateinit var promptListView: ListView<McpPromptWithServer>
    
    companion object {
        private const val PROMPT_LOAD_TIMEOUT_MS = 10000L
    }

    init {
        input {
            toolbar {
                textfield("") {
                    promptText = "Search"
                    setOnKeyPressed {
                        promptNameFilter = { text in it }
                        refilter()
                    }
                }
                spacer()
                button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                    tooltip("Refresh the prompt list.")
                    action { loadPrompts() }
                }
            }
            promptListView = listview(filteredPromptEntries) {
                vgrow = Priority.ALWAYS
                promptSelection.bind(this.selectionModel.selectedItemProperty())
                cellFormat {
                    graphic = Text("${it.prompt.name} [${it.serverName}]").apply {
                        tooltip(it.prompt.description ?: it.prompt.title)
                    }
                }
            }
        }
        outputPane.clear()
        output {
            find<McpPromptDetailsUi>().apply {
                visibleWhen(promptSelection.isNotNull)
                managedWhen(visibleProperty())
                promptSelection.onChange { promptWithServer.set(it) }
                this@output.add(this)
            }
        }
        hideParameters()
        hideRunButton()
        
        loadPrompts()
    }

    private fun loadPrompts() {
        // Clear existing entries before loading new ones
        promptEntries.clear()
        
        val serverNames = mcpController.mcpProviderRegistry.listProviderNames()
        
        // Launch a separate coroutine for each server to load prompts concurrently
        serverNames.forEach { serverName ->
            runAsync {
                try {
                    runBlocking {
                        withTimeout(PROMPT_LOAD_TIMEOUT_MS) {
                            val server = mcpController.mcpProviderRegistry.getProvider(serverName)
                            if (server != null) {
                                val prompts = server.listPrompts()
                                prompts.map { McpPromptWithServer(it, serverName) }
                            } else {
                                emptyList()
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    warning<McpPromptView>("Timeout loading prompts from server '$serverName' after ${PROMPT_LOAD_TIMEOUT_MS}ms")
                    emptyList()
                } catch (e: Exception) {
                    warning<McpPromptView>("Failed to load prompts from server '$serverName': ${e.message}")
                    emptyList()
                }
            } ui { prompts ->
                // Update the observable list incrementally as each server responds
                promptEntries.addAll(prompts)
                refilter()
            }
        }
    }

    private fun refilter() {
        filteredPromptEntries.setAll(promptEntries.filter { promptNameFilter(it.prompt.name) })
    }

    override suspend fun processUserInput() = AiPipelineResult.todo()
}
