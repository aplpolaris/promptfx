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
package tri.promptfx.prompts

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Model for filtering prompt trace objects. */
class PromptTraceFilter : Component() {
    val viewFilters = observableListOf<Pair<String, SimpleBooleanProperty>>()
    val modelFilters = observableListOf<Pair<String, SimpleBooleanProperty>>()
    val statusFilters = observableListOf(
        SUCCESS_STATUS to SimpleBooleanProperty(true),
        ERROR_STATUS to SimpleBooleanProperty(true),
        MISSING_VALUE_STATUS to SimpleBooleanProperty(false)
    )
    val typeFilters = observableListOf(
        INTERMEDIATE_RESULT to SimpleBooleanProperty(false),
        FINAL_RESULT to SimpleBooleanProperty(true),
        UNKNOWN to SimpleBooleanProperty(false)
    )
    val filter = SimpleObjectProperty<(AiPromptTraceSupport) -> Boolean> { true }

    init {
        statusFilters.forEach { it.second.onChange { updateFilter() } }
        typeFilters.forEach { it.second.onChange { updateFilter() } }
    }

    /** Update filter options based on given list of traces. */
    fun updateFilterOptions(list: List<AiPromptTraceSupport>) {
        updateFilterFlags(modelFilters, list.map { it.modelId }.distinct().sorted())
        updateFilterFlags(viewFilters, list.map { it.viewId }.distinct().sorted())
        updateFilter()
    }

    /** Update and return the current filter. */
    fun filter(): (AiPromptTraceSupport) -> Boolean {
        updateFilter()
        return filter.value!!
    }

    //region FILTER UPDATERS

    private fun updateFilterFlags(flags: ObservableList<Pair<String, SimpleBooleanProperty>>, values: List<String>) {
        (flags.map { it.first } - values.toSet()).toList().forEach { k ->
            flags.removeIf { it.first == k }
        }
        (values - flags.map { it.first }.toSet()).forEach {
            val newProp = SimpleBooleanProperty(true)
            newProp.onChange { updateFilter() }
            flags.add(it to newProp)
        }
    }

    private fun updateFilter() {
        val filterModel = createFilter(modelFilters) { it.modelId }
        val filterView = createFilter(viewFilters) { it.viewId }
        val filterStatus = createFilter(statusFilters) { it.statusId }
        val filterType = createFilter(typeFilters) { it.typeId }
        filter.set { filterModel(it) && filterView(it) && filterStatus(it) && filterType(it) }
    }

    private fun createFilter(flags: ObservableList<Pair<String, SimpleBooleanProperty>>, keyExtractor: (AiPromptTraceSupport) -> String): (AiPromptTraceSupport) -> Boolean {
        val selectedKeys = flags.filter { it.second.value }.map { it.first }.toSet()
        return { selectedKeys.contains(keyExtractor(it)) }
    }

    private val AiPromptTraceSupport.modelId
        get() = model?.modelId ?: UNKNOWN
    private val AiPromptTraceSupport.viewId
        get() = exec.viewId ?: UNKNOWN
    private val AiPromptTraceSupport.statusId
        get() = if (exec.error != null) ERROR_STATUS else if (output == null || firstValue == null) MISSING_VALUE_STATUS else SUCCESS_STATUS
    private val AiPromptTraceSupport.typeId
        get() = if (exec.intermediateResult == true) INTERMEDIATE_RESULT else if (exec.intermediateResult == false) FINAL_RESULT else UNKNOWN

    //endregion

    companion object {
        private const val INTERMEDIATE_RESULT = "intermediate result"
        private const val FINAL_RESULT = "final result"
        private const val UNKNOWN = "unknown"
        private const val SUCCESS_STATUS = "success"
        private const val ERROR_STATUS = "error"
        private const val MISSING_VALUE_STATUS = "missing value"
    }
}
