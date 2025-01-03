package tri.promptfx.api

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import tornadofx.Component
import tornadofx.observableListOf
import tornadofx.onChange
import tri.ai.core.ModelInfo
import tri.ai.core.ModelLifecycle
import tri.ai.core.ModelType

/** Model for filtering models. */
class ModelsFilter : Component() {
    val sourceFilters = observableListOf<Pair<String, SimpleBooleanProperty>>()
    val typeFilters = observableListOf<Pair<ModelType, SimpleBooleanProperty>>()
    val lifecycleFilters = observableListOf<Pair<ModelLifecycle, SimpleBooleanProperty>>()
    val filter = SimpleObjectProperty<(ModelInfo) -> Boolean> { true }

    /** Update filter options based on given list of traces. */
    fun updateFilterOptions(list: List<ModelInfo>) {
        updateFilterFlags(sourceFilters, list.map { it.source }.distinct().sorted())
        updateFilterFlags(typeFilters, list.map { it.type }.distinct())
        updateFilterFlags(lifecycleFilters, list.map { it.lifecycle }.distinct())
        updateFilter()
    }

    /** Update and return the current filter. */
    fun filter(): (ModelInfo) -> Boolean {
        updateFilter()
        return filter.value!!
    }

    //region FILTER UPDATERS

    private fun <X> updateFilterFlags(flags: ObservableList<Pair<X, SimpleBooleanProperty>>, values: List<X>) {
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
        val filterSource = createFilter(sourceFilters) { it.source }
        val filterType = createFilter(typeFilters) { it.type }
        val filterLifecycle = createFilter(lifecycleFilters) { it.lifecycle }
        filter.set { filterSource(it) && filterType(it) && filterLifecycle(it) }
    }

    private fun <X> createFilter(flags: ObservableList<Pair<X, SimpleBooleanProperty>>, keyExtractor: (ModelInfo) -> X): (ModelInfo) -> Boolean {
        val selectedKeys = flags.filter { it.second.value }.map { it.first }.toSet()
        return { selectedKeys.contains(keyExtractor(it)) }
    }

    fun selectAll() {
        sourceFilters.forEach { it.second.value = true }
        typeFilters.forEach { it.second.value = true }
        lifecycleFilters.forEach { it.second.value = true }
    }

    fun isAllSelected() =
        sourceFilters.all { it.second.value } && typeFilters.all { it.second.value } && lifecycleFilters.all { it.second.value }

    //endregion
}

/** Sort options for models. */
enum class ModelInfoSort(val comparator: Comparator<ModelInfo>) {
    ID_ASC(compareBy { it.id }),
    ID_DESC(compareByDescending { it.id }),
    CREATED_ASC(compareBy { it.created }),
    CREATED_DESC(compareByDescending { it.created }),
    SOURCE_ASC(compareBy { it.source }),
    SOURCE_DESC(compareByDescending { it.source }),
    STATUS_ASC(compareBy { it.lifecycle }),
    STATUS_DESC(compareByDescending { it.lifecycle }),
    TYPE_ASC(compareBy { it.type }),
    TYPE_DESC(compareByDescending { it.type });
}

/** Filter options for models. */
enum class ModelInfoFilter(val filter: (ModelInfo) -> Boolean) {
    SOURCE_OPENAI({ it.source == "OpenAI" }),
    SOURCE_GEMINI({ it.source == "Gemini" }),
    TYPE_TEXT_COMPLETION({ it.type == ModelType.TEXT_COMPLETION }),
    TYPE_TEXT_CHAT({ it.type == ModelType.TEXT_CHAT }),
    TYPE_TEXT_VISION_CHAT({ it.type == ModelType.TEXT_VISION_CHAT }),
    TYPE_TEXT_EMBEDDING({ it.type == ModelType.TEXT_EMBEDDING }),
    TYPE_IMAGE_GENERATOR({ it.type == ModelType.IMAGE_GENERATOR }),
    TYPE_TEXT_TO_SPEECH({ it.type == ModelType.TEXT_TO_SPEECH }),
    TYPE_SPEECH_TO_TEXT({ it.type == ModelType.SPEECH_TO_TEXT }),
    TYPE_MODERATION({ it.type == ModelType.MODERATION }),
    TYPE_QUESTION_ANSWER({ it.type == ModelType.QUESTION_ANSWER }),
    TYPE_UNKNOWN({ it.type == ModelType.UNKNOWN }),
    ALL({ true });

    companion object {
        fun ofType(type: ModelType) = valueOf("TYPE_${type.name}")
        fun ofSource(source: String) = valueOf("SOURCE_${source.uppercase()}")
    }
}
