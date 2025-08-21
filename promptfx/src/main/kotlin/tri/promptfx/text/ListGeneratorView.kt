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
package tri.promptfx.text

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPlanner
import tri.ai.pips.taskPlan
import tri.ai.prompt.PromptTemplate.Companion.INPUT
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxGlobals.promptsWithPrefix
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.promptfield
import tri.util.ui.MAPPER
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance

/** Plugin for the [ListGeneratorView]. */
class ListGeneratorPlugin : NavigableWorkspaceViewImpl<ListGeneratorView>("Text", "List Generator", WorkspaceViewAffordance.INPUT_ONLY, ListGeneratorView::class)

/** View designed to convert text to JSON. */
class ListGeneratorView: AiPlanTaskView("List Generator",
    "Enter text in the top box to generate a list of items/themes/topics.") {

    companion object {
        private const val PROMPT_PREFIX = "generate-list"
    }

    private val sourceText = SimpleStringProperty("")
    private val itemCategory = SimpleStringProperty("")
    private val sampleItems = SimpleStringProperty("")

    private val prompt = PromptSelectionModel(promptsWithPrefix(PROMPT_PREFIX).first())

    private val output = SimpleObjectProperty<ListPromptResult>()
    private val outputItems = observableListOf<String>()

    init {
        addInputTextArea(sourceText)
        parameters("Extraction Options") {
            field("Category") {
                tooltip("Provide the category of content for the list.")
                textfield(itemCategory)
            }
            field("Existing Items") {
                tooltip("Provide known/existing/sample items in the list, separated by commas.")
                textfield(sampleItems)
            }
            promptfield("Prompt", prompt, promptsWithPrefix(PROMPT_PREFIX), workspace)
        }
        addDefaultChatParameters(common)
    }

    init {
        output {
            listview(outputItems) {
                prefHeight = 200.0
                cellFormat {
                    text = it
                    val inNewItems = output.value?.new_items?.containsKey(it) ?: false
                    val newItemExplanation = output.value?.new_items?.get(it)
                    val inFoundItems = output.value?.items_in_input?.contains(it) ?: false
                    val inSampleItems = it in sampleItems.get().split(",").map { it.trim() }

                    if (inNewItems || (inFoundItems && !inSampleItems)) {
                        style = "-fx-font-weight: bold;"
                    }
                    if (newItemExplanation != null) {
                        tooltip(newItemExplanation)
                    } else {
                        tooltip(it)
                    }
                }
                lazyContextmenu {
                    item("Add to Known Items") {
                        enableWhen(selectionModel.selectedItemProperty().booleanBinding(sampleItems) {
                            it != null && it !in sampleItems.get().split(",")
                        })
                        action {
                            sampleItems.set((sampleItems.get().trim() + ", ${selectionModel.selectedItem}").removePrefix(", "))
                        }
                    }
                }
            }
        }

        onCompleted {
            val rawText = it.finalResult.firstValue.toString()
            val codeText = if ("```json" in rawText)
                rawText.substringAfter("```json").substringBefore("```").trim()
            else
                rawText.substringAfter("```").substringBefore("```").trim()
            try {
                val parsed1 = MAPPER.readValue<ListPromptResult>(codeText)
                output.set(parsed1)
                outputItems.setAll(parsed1.items_in_input)
            } catch (x: JsonMappingException) {
                val parsed2 = MAPPER.readValue<Map<String, Any>>(codeText)
                outputItems.setAll((parsed2["items_in_input"] as? List<String>) ?: listOf())
            }
        }
    }

    override fun plan(): AiPlanner {
        runLater {
            output.set(null)
            outputItems.clear()
        }
        return common.completionBuilder()
            .prompt(prompt.prompt.value)
            .params(INPUT to sourceText.get(), "item_category" to itemCategory.get(), "known_items" to sampleItems.get().parseSampleItems())
            .requestJson(true)
            .taskPlan(chatEngine)
    }

    private fun String.parseSampleItems() =
        split(",").joinToString(prefix = "[", separator = ",", postfix = "]") { "\"${it.trim()}\"" }

    /** Object describing result of a list prompt. */
    class ListPromptResult(
        val item_category: String,
        val known_items: List<String>? = listOf(),
        val items_in_input: List<String>,
        val new_items: Map<String, String>? = mapOf()
    )

}
