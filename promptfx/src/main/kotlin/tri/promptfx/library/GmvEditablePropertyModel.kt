package tri.promptfx.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import tri.ai.text.chunks.TextDocMetadata
import tri.ai.text.chunks.process.GuessedMetadataObject
import tri.ai.text.chunks.process.MultipleGuessedMetadataObjects
import tri.ai.text.chunks.process.PdfMetadataGuesser.toGuessedMetadataObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A key-value property that can be edited and reverted to an original value.
 * The original value can be "null" indicating the property was added, and the editing value can be "null" indicating the property was deleted.
 * Any change can be marked "accepted" to indicate it should be saved to the document when the user applies changes.
 * Also supports cycling through a list of custom alternate values.
 */
class GmvEditablePropertyModel<X>(
    val name: String,
    _originalValue: X?,
    _editingValue: X?,
    valueOptions: List<Pair<X, String>>,
    val isEditable: Boolean = true
) {

    /** The original value. */
    val originalValue = SimpleObjectProperty<X>(_originalValue)
    /** A user-provided custom value, if not in provided list of options. */
    private val customValue = SimpleObjectProperty<X>(if (valueOptions.any { it.first == _editingValue }) null else _editingValue)
    /** An ordered list of alternate values, pairing a value with the source of the value. */
    private val valueOptionList = observableListOf(valueOptions.toList())

    /** The initial editing value. */
    private val initialEditingValue = _editingValue ?: _originalValue
    /** The value currently presented to the user as editing. */
    val editingValue = SimpleObjectProperty<X>(initialEditingValue)

    /** If possible to mark this property as "deleted". */
    val isDeletable: BooleanBinding = originalValue.isNotNull and isEditable
    /** If property has been marked as deleted. */
    val isDeletePending = SimpleBooleanProperty(false)

    /** If non-deletion edit has been marked as accepted by user. */
    val isUpdatePending = SimpleBooleanProperty(false)
    /** If a change of any kind is pending. */
    val isAnyChangePending: BooleanBinding = isUpdatePending or isDeletePending

    //region DERIVED PROPERTIES

    /** If editing value is the original value. */
    val isOriginal = editingValue.isEqualTo(originalValue) and !isDeletePending
    /** If editing value is the custom value. */
    val isCustom = editingValue.isEqualTo(customValue) and !isDeletePending
    /** If editing value is in the list of value options. */
    val isValueOption = editingValue.booleanBinding(valueOptionList) { value ->
        valueOptionList.any { it.first == value }
    }

    /** List used for cycling values. */
    private val valueCycleList = observableListOf<Pair<X, String>>().apply {
        fun update() {
            val tempList = mutableListOf<Pair<X, String>>()
            if (originalValue.value != null)
                tempList.add(originalValue.value to "Original")
            if (customValue.value != null)
                tempList.add(customValue.value to "Custom")
            tempList.addAll(valueOptionList)
            setAll(tempList)
        }
        originalValue.onChange { update() }
        customValue.onChange { update() }
        valueOptionList.onChange { update() }
        update()
    }
    /** Index of the editing value in the list of value options. When updated, changes the editing value. */
    private val initialEditIndex = if (editingValue.value == null) 0 else valueCycleList.indexOfFirst { it.first == editingValue.value }
    internal val editingIndex = SimpleIntegerProperty(initialEditIndex).apply {
        onChange { editingValue.set(valueCycleList[it]!!.first) }
        valueCycleList.onChange { set(0) }
    }
    /** Flag indicating whether value cycling is supported. */
    val isValueCyclable = valueCycleList.sizeProperty.ge(2)

    /** Label indicating whether value has been changed. */
    val changeLabel = editingIndex.stringBinding(valueCycleList, isDeletePending, isOriginal, isUpdatePending) {
        val label = valueCycleList.getOrNull(editingIndex.value)?.second
        if (isDeletePending.value)
            "Marked for Deletion"
        else if (isUpdatePending.value)
            "Marked for Update - $label"
        else if (originalValue.value == null)
            "New - $label"
        else if (isOriginal.value && label == "Original")
            "Original"
        else if (isOriginal.value)
            "Unchanged - $label"
        else
            "Changed - $label"
    }

    /** Return true if both the original and editing values are empty or blank. */
    fun hasNonEmptyValues(): Boolean {
        val originalEmpty = originalValue.value.let { it == null || (it is String && it.isBlank()) }
        val editingEmpty = editingValue.value.let { it == null || (it is String && it.isBlank()) }
        return !originalEmpty || !editingEmpty
    }

    /** Get list of additional values. */
    fun getAlternateValueList() =
        valueOptionList.toList()

    //endregion

    //region MUTATORS

    /** Updates with a custom value. */
    fun updateCustom(value: X) {
        customValue.set(value)
        editingIndex.set(if (originalValue.value == null) 0 else 1)
    }

    /** Applies changes, replacing the original value with the editing value. */
    fun applyChanges() {
        require(editingValue.value != null) { "Changes should only be applied when the editing value is not null." }
        require(!isDeletePending.value) { "Changes should only be applied when the property is not marked for deletion." }
        originalValue.set(editingValue.value)
        editingIndex.set(0)
        isUpdatePending.set(false)
    }

    /** Reverts the editing value to the original value. */
    fun revertChanges() {
        editingValue.set(initialEditingValue)
        isUpdatePending.set(false)
        isDeletePending.set(false)
    }

    /** Adds to list of additional values. */
    fun addAlternateValues(values: List<Pair<X, String>>, filterUnique: Boolean) {
        if (filterUnique) {
            val curValues = setOfNotNull(originalValue.value) + valueOptionList.mapNotNull { it.first }
            valueOptionList.addAll(values.filter { it.first !in curValues })
        } else {
            valueOptionList.addAll(values)
        }
    }

    /** Move to the previous value. */
    fun previousValue() {
        editingIndex.set((editingIndex.value - 1 + valueCycleList.size) % valueCycleList.size)
    }

    /** Move to the next value. */
    fun nextValue() {
        editingIndex.set((editingIndex.value + 1) % valueCycleList.size)
    }

    //endregion

}

/** Convert TextDocMetadata to a list of GMVs. */
fun TextDocMetadata.asGmvPropList(label: String, isOriginal: Boolean) =
    MultipleGuessedMetadataObjects(toGuessedMetadataObject(label), listOf()).asGmvPropList(isOriginal)

/**
 * Convert GMO to a list of GMVs. Initial values are set to the first object, and the remaining values are used as alternates.
 * Properties whose value in the first object is null are not included.
 */
fun MultipleGuessedMetadataObjects.asGmvPropList(isOriginal: Boolean): List<GmvEditablePropertyModel<Any?>> {
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
    fun propertyModel(key: String, op: (GuessedMetadataObject) -> Any?): GmvEditablePropertyModel<Any?> {
        val comb = op(combined)
        val original = if (isOriginal) comb else null
        val editing = if (isOriginal) null else comb
        return GmvEditablePropertyModel(key, original, editing, labeledValues(op))
    }

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
        it.hasNonEmptyValues()
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