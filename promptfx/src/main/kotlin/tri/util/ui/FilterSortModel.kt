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
package tri.util.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.observableListOf
import tornadofx.onChange

/**
 * Generic model for filtering and sorting by a set of attributes.
 * Every attribute value is maintained as a separate [SimpleBooleanProperty].
 */
abstract class FilterSortModel<X> {

    /** Tracks checked status of individual attributes. */
    val filters = mutableMapOf<String, AttributeFilter<X, *>>()
    /** Tracks combined filter for all attributes. */
    val filter = SimpleObjectProperty<(X) -> Boolean> { true }
    /** Attribute sort operations, from most significant to least. */
    val sortOps = mutableListOf<AttributeComparator<X, *>>()
    /** Tracks compound sort order for the list. */
    val sort = SimpleObjectProperty<Comparator<X>>(compareBy { it.toString() })

    //region INITIALIZERS FOR SUB-CLASSES

    protected fun <Y> addFilter(key: String, attribute: (X) -> Y) {
        filters[key] = AttributeFilter(attribute)
    }

    //endregion

    /** Update filter options based on given list of objects. */
    open fun updateFilterOptions(list: List<X>) {
        filters.forEach { (_, f) ->
            f.updateAttributeOptions(list.toSet(), ::updateFilter)
        }
        updateFilter()
    }

    /** Invoked whenever one of the flag properties changes, resets filter to match all flag filters. */
    open fun updateFilter() {
        val flagFilters = filters.map { (_, value) -> value.createFilter() }
        filter.set { x -> flagFilters.all { it(x) } }
    }

    fun <Y> sortBy(key: String, attribute: (X) -> Y, sortAscend: Boolean = true) {
        sortOps.removeIf { it.key == key }
        sortOps.add(0, AttributeComparator(key, attribute, sortAscend))
        updateSort()
    }

    /** Reset the sort operation. */
    fun resetSort() {
        sortOps.clear()
        updateSort()
    }

    /** Update sort operation based on current sortOps. */
    private fun updateSort() {
        if (sortOps.isEmpty())
            sort.set(compareBy { it.toString() })
        else
            sort.set(sortOps.map { it.comparator() }.reduce { acc, c -> acc.thenComparing(c) })
    }

    open fun selectAll() = filters.values.forEach { it.selectAll() }
    open fun selectNone() = filters.values.forEach { it.selectNone() }

    open fun isAllSelected() = filters.values.all { it.isAllSelected() }
    open fun isNoneSelected() = filters.values.all { it.isNoneSelected() }

}

/**
 * Tracks [SimpleBooleanProperty] for a dynamic list of attribute values.
 */
class AttributeFilter<X, Y>(val attribute: (X) -> Y) {
    val values = observableListOf<Pair<Y, SimpleBooleanProperty>>()

    /** Updates filter options based on given list of values. */
    fun updateAttributeOptions(newItems: Set<X>, updateFilter: () -> Unit) {
        val newFlags = newItems.map(attribute).toSet()
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

    /** Generate a filter for matching attributes. */
    fun createFilter(): (X) -> Boolean {
        val selectedFlags = values.filter { it.second.value }.map { it.first }.toSet()
        return { selectedFlags.contains(attribute(it)) }
    }

    fun selectAll() { values.forEach { it.second.value = true } }
    fun selectNone() { values.forEach { it.second.value = false } }
    fun isAllSelected() = values.all { it.second.value }
    fun isNoneSelected() = values.none { it.second.value }
}

/**
 * An attribute sort operation.
 */
class AttributeComparator<X, Y>(val key: String, val attribute: (X) -> Y, val sortAscend: Boolean) {
    fun comparator(): Comparator<X> = when {
        sortAscend -> compareBy { attribute(it)?.let { it as? Comparable<*> ?: it.toString() } }
        else -> compareByDescending { attribute(it)?.let { it as? Comparable<*> ?: it.toString() } }
    }
}
