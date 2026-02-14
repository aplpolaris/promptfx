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
package tri.promptfx.api

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import tornadofx.Component
import tornadofx.observableListOf
import tornadofx.onChange
import tri.ai.core.DataModality
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import tri.util.ui.FilterSortModel

/** Model for filtering models. */
class ModelsFilter : Component() {

    // Custom modality filters that work differently from standard filters
    val inputModalityFilter = ModalityFilter { it.inputs ?: emptyList() }
    val outputModalityFilter = ModalityFilter { it.outputs ?: emptyList() }

    val model = object : FilterSortModel<ModelInfo>() {
        override fun updateFilterOptions(list: List<ModelInfo>) {
            // Handle standard filters
            super.updateFilterOptions(list)
            // Handle custom modality filters
            inputModalityFilter.updateAttributeOptions(list.toSet()) { updateFilter() }
            outputModalityFilter.updateAttributeOptions(list.toSet()) { updateFilter() }
        }
        
        override fun updateFilter() {
            val standardFilters = filters.map { (_, value) -> value.createFilter() }
            val inputFilter = inputModalityFilter.createFilter()
            val outputFilter = outputModalityFilter.createFilter()
            val allFilters = standardFilters + inputFilter + outputFilter
            filter.set { x -> allFilters.all { it(x) } }
        }
        
        override fun selectAll() {
            super.selectAll()
            inputModalityFilter.selectAll()
            outputModalityFilter.selectAll()
        }
        
        override fun isAllSelected(): Boolean = 
            super.isAllSelected() && inputModalityFilter.isAllSelected() && outputModalityFilter.isAllSelected()
        
        init {
            addFilter("source") { it.source }
            addFilter("type") { it.type }
            addFilter("lifecycle") { it.lifecycle }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val sourceFilters: ObservableList<Pair<String, SimpleBooleanProperty>>
        get() = model.filters["source"]!!.values as ObservableList<Pair<String, SimpleBooleanProperty>>
    @Suppress("UNCHECKED_CAST")
    val typeFilters
        get() = model.filters["type"]!!.values as ObservableList<Pair<ModelType, SimpleBooleanProperty>>
    @Suppress("UNCHECKED_CAST")
    val lifecycleFilters
        get() = model.filters["lifecycle"]!!.values as ObservableList<Pair<String, SimpleBooleanProperty>>
    
    val inputFilters
        get() = inputModalityFilter.values
    
    val outputFilters
        get() = outputModalityFilter.values

}

/**
 * Custom filter for input/output modalities that handles list containment.
 * Unlike regular filters, this checks if any of the selected modalities are present in the model's list.
 */
class ModalityFilter(val attribute: (ModelInfo) -> List<DataModality>) {
    val values = observableListOf<Pair<DataModality, SimpleBooleanProperty>>()

    /** Updates filter options based on given list of values. */
    fun updateAttributeOptions(newItems: Set<ModelInfo>, updateFilter: () -> Unit) {
        val newFlags = newItems.flatMap(attribute).toSet()
        val curFlags = values.map { it.first }.toSet()
        (curFlags - newFlags).forEach { k ->
            values.removeIf { it.first == k }
        }
        (newFlags - curFlags).forEach {
            val newProp = SimpleBooleanProperty(true)
            newProp.onChange { updateFilter() }
            values.add(it to newProp)
        }
    }

    /** Generate a filter for matching attributes - returns true if model contains any selected modalities. */
    fun createFilter(): (ModelInfo) -> Boolean {
        val selectedFlags = values.filter { it.second.value }.map { it.first }.toSet()
        if (selectedFlags.isEmpty()) return { true } // If no modalities selected, show all
        return { model -> 
            val modelModalities = attribute(model)
            modelModalities.any { it in selectedFlags }
        }
    }

    fun selectAll() { values.forEach { it.second.value = true } }
    fun selectNone() { values.forEach { it.second.value = false } }
    fun isAllSelected() = values.all { it.second.value }
    fun isNoneSelected() = values.none { it.second.value }
}
