package tri.promptfx.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import javafx.beans.binding.Binding
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import tornadofx.*
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.process.GuessedMetadataObject
import tri.ai.text.chunks.process.MultipleGuessedMetadataObjects
import tri.ai.text.chunks.process.PdfMetadataGuesser.toGuessedMetadataObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A single GMV property, with three possible value types.
 * Supports notions of a current value, an edited value, and a collection of alternate values.
 */
class GmvEditablePropertyModel<X>(
    val name: String,
    initialValue: X,
    val isEditable: Boolean = true
) {

    constructor(label: String, initialValue: X, altValues: List<Pair<X, String>>) : this(label, initialValue, true) {
        alternateValues.addAll(altValues)
        editingIndex.set(alternateValues.indexOfFirst { it.first == initialValue })
    }

    /** An ordered list of alternate values, pairing a value with the source of the value. */
    private val alternateValues = observableListOf<Pair<X, String>>()

    /** The saved value. */
    val savedValue = SimpleObjectProperty<X>(null)
    /** The value currently presented to the user as editing (ready to be saved). */
    val editingValue = SimpleObjectProperty<X>(initialValue)

    //region DERIVED PROPERTIES

    /** Flag indicating whether value cycling is supported. */
    val supportsValueCycling = alternateValues.sizeProperty.ge(2)
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
    /** Flag indicating whether value is saved. */
    val isSaved: BooleanBinding = savedValue.isNotNull and editingValue.isEqualTo(savedValue)
    /** The source of the editing value. */
    val editingSource: ObservableValue<String> = editingIndex.objectBinding(isSaved) { x ->
        when {
            x == -1 -> ""
            else -> alternateValues[x!!.toInt()].second
        }
    } as Binding<String>

    /** Label indicating whether value has been saved. */
    val savedLabel = editingSource.stringBinding(editingIndex) {
        if (editingIndex.value == -1)
            "Saved"
        else
            "Unsaved - $it"
    }

    //endregion

    //region MUTATORS

    /** Get list of additional values. */
    fun getAlternateValueList() =
        alternateValues.toList()

    /** Adds to list of additional values. */
    fun addAlternateValues(values: List<Pair<X, String>>, filterUnique: Boolean) {
        if (filterUnique) {
            val curValues = setOfNotNull(savedValue.value) + alternateValues.mapNotNull { it.first }
            alternateValues.addAll(values.filter { it.first !in curValues })
        } else {
            alternateValues.addAll(values)
        }
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
        updateEditingIndex(newIndex)
    }

    /** Move to the next value. */
    fun nextValue() {
        val newIndex = when (editingIndex.value) {
            alternateValues.size - 1 -> if (savedValue.value == null) 0 else -1
            else -> editingIndex.value + 1
        }
        updateEditingIndex(newIndex)
    }

    private fun updateEditingIndex(newIndex: Int) {
        editingIndex.set(newIndex)
        if (editingIndex.value == -1)
            editingValue.set(savedValue.value)
        else
            editingValue.set(alternateValues[editingIndex.value].first)
    }

    //endregion

}

/** Convert TextDocMetadata to a list of GMVs. */
fun TextDocMetadata.asGmvPropList(label: String, markSaved: Boolean) =
    MultipleGuessedMetadataObjects(toGuessedMetadataObject(label), listOf()).asGmvPropList(markSaved)

/**
 * Convert GMO to a list of GMVs. Initial values are set to the first object, and the remaining values are used as alternates.
 * Properties whose value in the first object is null are not included.
 */
fun MultipleGuessedMetadataObjects.asGmvPropList(markSaved: Boolean): List<GmvEditablePropertyModel<Any?>> {
    // helper function to generate a list of values that are non-empty
    fun labeledValues(op: (GuessedMetadataObject) -> Any?): List<Pair<Any?, String>> {
        val result = mutableListOf<Pair<Any?, String>>()
        sources.forEach {
            val value = op(it)
            val isBlank = value == null || value.toString().isBlank()
                    || (value as? List<*>)?.isEmpty() == true
                    || (value as? Map<*,*>)?.isEmpty() == true
            if (!isBlank)
                result.add(value to it.label)
        }
        if (result.size > 1)
            result.add(0, op(combined) to combined.label)
        return result
    }

    // helper function to generate a property model
    fun propertyModel(key: String, op: (GuessedMetadataObject) -> Any?): GmvEditablePropertyModel<Any?> =
        GmvEditablePropertyModel(key, op(combined), labeledValues(op))

    return mutableListOf<GmvEditablePropertyModel<Any?>>().apply {
        add(propertyModel("title") { it.title })
        add(propertyModel("subtitle") { it.subtitle })
        add(propertyModel("authors") { it.authors })
        add(propertyModel("date") { it.date?.tryParseLocalDate() })
        add(propertyModel("keywords") { it.keywords })
        add(propertyModel("abstract") { it.abstract })
        add(propertyModel("executiveSummary") { it.executiveSummary })
        add(propertyModel("sections") { it.sections })
        add(propertyModel("captions") { it.captions })
        add(propertyModel("references") { it.references })

        combined.other.forEach { (k, _) ->
            add(propertyModel(k) { it.other[k].maybeParseDate(k) })
        }
    }.filter {
        it.editingValue.value != null
    }.onEach {
        if (markSaved)
            it.saveValue()
    }
}

//region OBJECT TYPE HELPERS

internal fun Any?.maybeParseDate(key: String) =
    if ("date" in key.lowercase())
        maybeParseDate()
    else this

internal fun Any?.maybeParseDate() = when {
    this == null ->
        null
    this is String ->
        tryParseLocalDate()
    this is List<*> && all { it is Number } ->
        (this as List<Number>).tryParseLocalDate()
    else ->
        this
}

internal fun String.tryParseLocalDate() =
    try { LocalDate.parse(this) } catch (e: Exception) {
        try { LocalDateTime.parse(this).toLocalDate() } catch (e: Exception) { this }
    }

internal fun List<Number>.tryParseLocalDate() =
    try { MAPPER.convertValue(this, LocalDate::class.java) } catch (e: Exception) {
        try { MAPPER.convertValue(this, LocalDateTime::class.java).toLocalDate() } catch (e: Exception) { this }
    }

private val MAPPER = ObjectMapper().registerModule(JavaTimeModule())

//endregion