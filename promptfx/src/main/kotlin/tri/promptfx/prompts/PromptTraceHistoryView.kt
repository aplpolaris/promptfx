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
package tri.promptfx.prompts

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.AiTaskView
import tri.promptfx.ui.PromptResultArea
import tri.promptfx.ui.prompt.PromptTraceCardList
import tri.promptfx.ui.prompt.PromptTraceDetailsUi
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [PromptTemplateView]. */
class PromptTraceHistoryPlugin : NavigableWorkspaceViewImpl<PromptTraceHistoryView>("Prompts", "Prompt Trace History", type = PromptTraceHistoryView::class)

/** A view designed to help you test prompt templates. */
class PromptTraceHistoryView : AiTaskView("Prompt Trace History", "View and export history of prompt executions.") {

    private val promptListUi = find<PromptTraceCardList>(
        "prompts" to controller.promptHistory.prompts,
        "isShowFilter" to true,
        "isRemovable" to true,
        "toolbarLabel" to "Prompt Traces:"
    )
    private val isShowFormattedOutput = SimpleBooleanProperty(false)
    private val isShowDetailedOutput = SimpleBooleanProperty(true)

    init {
        hideRunButton()
        hideParameters()
        input {
            add(promptListUi)
        }
        outputPane.clear()
        output {
            toolbar {
                label("View as:")
                togglegroup {
                    radiobutton("Trace Details") {
                        isSelected = true
                        selectedProperty().onChange { isShowDetailedOutput.set(it) }
                    }
                    radiobutton("Formatted Result") {
                        isSelected = false
                        selectedProperty().onChange { isShowFormattedOutput.set(it) }
                    }
                }
            }
            find<PromptResultArea>().apply {
                with (root) {
                    visibleWhen(isShowFormattedOutput)
                    managedWhen(isShowFormattedOutput)
                }
                promptListUi.selectedPrompt.onChange {
                    model.clearTraces()
                    if (it != null)
                        model.addTrace(it)
                }
                this@output.add(this)
            }
            find<PromptTraceDetailsUi>().apply {
                with (root) {
                    isManaged = false
                    isVisible = false
                    visibleWhen(isShowDetailedOutput)
                    managedWhen(isShowDetailedOutput)
                }
                promptListUi.selectedPrompt.onChange {
                    setTrace(it ?: AiPromptTrace<String>())
                }
                this@output.add(this)
            }
        }
    }

    fun selectPromptTrace(prompt: AiPromptTraceSupport<*>) {
        // TODO - objects are edited when they reach history, this could be done better
        val foundPrompt = promptListUi.prompts.firstOrNull {
            it.prompt == prompt.prompt && it.model == prompt.model && it.output == prompt.output
        }
        promptListUi.selectPromptTrace(foundPrompt ?: prompt)
    }

    override suspend fun processUserInput() = AiPipelineResult.todo<String>()
}

