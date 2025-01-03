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

    protected fun <Y> addFilter(key: String, flagExtractor: (X) -> Y) {
        filters[key] = AttributeFilter(flagExtractor)
    }

    /** Update filter options based on given list of objects. */
    fun updateFilterOptions(list: List<X>) {
        filters.forEach { (_, f) ->
            f.updateAttributeOptions(list.toSet(), ::updateFilter)
        }
        updateFilter()
    }

    /** Invoked whenever one of the flag properties changes, resets filter to match all flag filters. */
    fun updateFilter() {
        val flagFilters = filters.map { (_, value) -> value.createFilter() }
        filter.set { x -> flagFilters.all { it(x) } }
    }

    fun selectAll() = filters.values.forEach { it.selectAll() }
    fun selectNone() = filters.values.forEach { it.selectNone() }

    fun isAllSelected() = filters.values.all { it.isAllSelected() }
    fun isNoneSelected() = filters.values.all { it.isNoneSelected() }

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