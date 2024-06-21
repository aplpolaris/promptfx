package tri.promptfx.library

import javafx.beans.binding.Binding
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import tornadofx.*

/**
 * A single GMV property, with three possible value types.
 * Supports notions of a current value, an edited value, and a collection of alternate values.
 */
class GmvEditablePropertyModel<X>(
    val label: String,
    initialValue: X,
    val isEditable: Boolean = true
) {

    constructor(label: String, initialValue: X, altValues: List<Pair<X, String>>) : this(label, initialValue, true) {
        alternateValues.addAll(altValues)
    }

    /** An ordered list of alternate values, pairing a value with the source of the value. */
    private val alternateValues = observableListOf<Pair<X, String>>()

    /** The saved value. */
    val savedValue = SimpleObjectProperty<X>(null)
    /** The value currently presented to the user as editing (ready to be saved). */
    val editingValue = SimpleObjectProperty<X>(initialValue)

    //region DERIVED PROPERTIES

    /** Label indicating whether value has been saved. */
    val savedLabel = savedValue.stringBinding { if (it == null) "Unsaved" else "Saved" }

    /** Flag indicating whether value cycling is supported. */
    val supportsValueCycling = alternateValues.sizeProperty.ge(2)
    /** Flag indicating whether value is saved. */
    val isSaved: BooleanBinding = savedValue.isEqualTo(editingValue)
    /** The index of the editing value in the list of alternate values, or -1 if not present. */
    val editingIndex = SimpleIntegerProperty(-1).apply {
        alternateValues.onChange {
            val valueAtIndex = alternateValues.getOrNull(value)
            if (valueAtIndex != null && valueAtIndex.first != editingValue.value)
                set(alternateValues.indexOfFirst { it.first == editingValue.value })
        }
        editingValue.onChange {
            val valueAtIndex = alternateValues.getOrNull(value)
            if (valueAtIndex != null && valueAtIndex.first != editingValue.value)
                set(alternateValues.indexOfFirst { it.first == editingValue.value })
        }
    }
    /** The source of the editing value. */
    val editingSource: ObservableValue<String> = editingIndex.objectBinding(isSaved) { x ->
        when {
            x == -1 -> "Custom"
            else -> alternateValues[x!!.toInt()].second
        }
    } as Binding<String>

    //endregion

    //region MUTATORS

    /** Get list of additional values. */
    fun getAlternateValueList() =
        alternateValues.toList()

    /** Adds to list of additional values. */
    fun addAlternateValues(values: List<Pair<X, String>>) {
        alternateValues.addAll(values)
    }

    /** Save the current value. */
    fun saveValue() {
        savedValue.set(editingValue.value)
        editingIndex.set(-1)
    }

    /** Move to the previous value. */
    fun previousValue() {
        val newIndex = when (editingIndex.value) {
            -1 -> alternateValues.size - 1
            0 -> if (savedValue.value == null) alternateValues.size - 1 else -1
            else -> editingIndex.value - 1
        }
        editingIndex.set(newIndex)
        editingValue.set(alternateValues[editingIndex.value].first)
    }

    /** Move to the next value. */
    fun nextValue() {
        val newIndex = when (editingIndex.value) {
            alternateValues.size - 1 -> if (savedValue.value == null) 0 else -1
            else -> editingIndex.value + 1
        }
        editingIndex.set(newIndex)
        editingValue.set(alternateValues[editingIndex.value].first)
    }

    //endregion

}