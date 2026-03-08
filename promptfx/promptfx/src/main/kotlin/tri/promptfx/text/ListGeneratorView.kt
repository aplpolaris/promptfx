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
package tri.promptfx.text

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.core.CompletionBuilder
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.promptfx.AiChatEngine
import tri.promptfx.execute
import tri.promptfx.taskPlan
import tri.ai.prompt.PromptTemplate.Companion.INPUT
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.JsonListMergeStrategy
import tri.ai.prompt.trace.mergeJsonLists
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxGlobals.promptsWithPrefix
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.promptfield
import tri.util.ui.MAPPER
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.slider
import tri.util.ui.sliderwitheditablelabel

/** Plugin for the [ListGeneratorView]. */
class ListGeneratorPlugin : NavigableWorkspaceViewImpl<ListGeneratorView>("Text", "Convert to List", WorkspaceViewAffordance.INPUT_ONLY, ListGeneratorView::class)

/** View designed to convert text to JSON. */
class ListGeneratorView: AiPlanTaskView("Convert to List",
    "Enter text in the top box to generate a list of items/themes/topics.") {

    companion object {
        private const val PROMPT_PREFIX = "generate-list"
    }

    private val sourceText = SimpleStringProperty("")
    private val itemCategory = SimpleStringProperty("")
    private val sampleItems = SimpleStringProperty("")

    private val prompt = PromptSelectionModel(promptsWithPrefix(PROMPT_PREFIX).first())

    private val numAttempts = SimpleIntegerProperty(1)
    private val mergeStrategy = SimpleObjectProperty(JsonListMergeStrategy.TOP_REPEATED)
    private val minConsensus = SimpleDoubleProperty(0.5)

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
            field("Attempts") {
                tooltip("Number of independent model queries to run and merge into a final result.")
                sliderwitheditablelabel(1..10, numAttempts)
            }
            field("Merge Strategy") {
                tooltip("Strategy for combining items across multiple attempts.")
                combobox(mergeStrategy, JsonListMergeStrategy.values().toList())
            }.enableWhen(numAttempts.greaterThan(1))
            field("Min. Consensus") {
                tooltip("Minimum fraction of attempts that must agree on an item for it to be included (only applies to TOP_REPEATED strategy).")
                slider(0.0..1.0, minConsensus)
                label(minConsensus.asString("%.2f"))
            }.enableWhen(numAttempts.greaterThan(1).and(mergeStrategy.isEqualTo(JsonListMergeStrategy.TOP_REPEATED)))
        }
        parameters("Chat Model") {
            field("Model") {
                combobox(controller.chatEngine, PromptFxModels.chatEngines())
            }
            with(common) {
                temperature()
                maxTokens()
            }
            field("Number of Responses") {
                disableWhen(numAttempts.greaterThan(1))
                tooltip("Number of responses per request. Disabled when using multiple attempts.")
                slider(1..10, common.numResponses)
                label(common.numResponses.asString())
            }
        }
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
            val firstValue = it.finalResult.firstValue
            // Multi-attempt result: `other` holds the merged ListPromptResult
            val mergedResult = firstValue.other as? ListPromptResult
            if (mergedResult != null) {
                output.set(mergedResult)
                outputItems.setAll(mergedResult.items_in_input)
                return@onCompleted
            }
            // Single attempt: parse the full JSON result
            val rawText = firstValue.textContent()
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
        val builder = common.completionBuilder()
            .numResponses(1)
            .prompt(prompt.prompt.value)
            .params(INPUT to sourceText.get(), "item_category" to itemCategory.get(), "known_items" to sampleItems.get().parseSampleItems())
            .requestJson(true)
        return if (numAttempts.value > 1) {
            val localAttempts = numAttempts.value
            val localStrategy = mergeStrategy.value
            val localMinConsensus = minConsensus.value
            val localChat = chatEngine
            aitask("list-generator-multi-attempt") {
                mergeAttempts(builder, localChat, localAttempts, localStrategy, localMinConsensus)
            }.planner
        } else {
            builder.taskPlan(chatEngine)
        }
    }

    /** Runs [attempts] independent LLM calls, parses each as a [ListPromptResult], and merges them. */
    private suspend fun mergeAttempts(
        builder: CompletionBuilder,
        engine: AiChatEngine,
        attempts: Int,
        strategy: JsonListMergeStrategy,
        minConsensus: Double
    ): AiPromptTrace {
        builder.requestJson(true)
        val startTime = System.currentTimeMillis()
        val traces = (1..attempts).map { builder.execute(engine) }
        val totalTime = System.currentTimeMillis() - startTime

        val totalQueryTokens = traces.sumOf { it.exec.queryTokens ?: 0 }.takeIf { it > 0 }
        val totalResponseTokens = traces.sumOf { it.exec.responseTokens ?: 0 }.takeIf { it > 0 }
        val baseExecInfo = AiExecInfo(
            attempts = attempts,
            responseTimeMillisTotal = totalTime,
            queryTokens = totalQueryTokens,
            responseTokens = totalResponseTokens
        )

        val successful = traces.filter { it.exec.succeeded() }
        if (successful.isEmpty()) {
            val last = traces.last()
            return AiPromptTrace(last.prompt, last.model, baseExecInfo.copy(error = last.exec.error, throwable = last.exec.throwable))
        }

        val parsedResults = successful.mapNotNull { trace ->
            val rawText = trace.values?.firstOrNull()?.let { it.text ?: it.message?.content } ?: return@mapNotNull null
            val codeText = when {
                "```json" in rawText -> rawText.substringAfter("```json").substringBefore("```").trim()
                "```" in rawText -> rawText.substringAfter("```").substringBefore("```").trim()
                else -> rawText
            }
            try { MAPPER.readValue<ListPromptResult>(codeText) } catch (_: Exception) { null }
        }
        if (parsedResults.isEmpty()) {
            val last = successful.last()
            return AiPromptTrace(last.prompt, last.model, baseExecInfo.copy(error = "No valid list result from ${successful.size} of $attempts attempt(s)"))
        }

        // Merge: metadata from first result; items_in_input with strategy; new_items union filtered to merged items
        val first = parsedResults.first()
        val mergedItems = mergeJsonLists(parsedResults.map { it.items_in_input }, strategy, minConsensus)
        val mergedItemsLower = mergedItems.map { it.lowercase() }.toSet()
        val mergedNewItems = parsedResults
            .flatMap { it.new_items?.entries ?: emptySet() }
            .filter { it.key.lowercase() in mergedItemsLower }
            .distinctBy { it.key.lowercase() }
            .associate { it.key to it.value }

        val merged = ListPromptResult(
            item_category = first.item_category,
            known_items = first.known_items,
            items_in_input = mergedItems,
            new_items = mergedNewItems
        )
        val last = successful.last()
        return AiPromptTrace(last.prompt, last.model, baseExecInfo, AiOutputInfo.other(merged))
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
