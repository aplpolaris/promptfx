package tri.promptfx.api

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import tornadofx.Component
import tri.ai.core.ModelInfo
import tri.ai.core.ModelType
import tri.util.ui.FilterSortModel

/** Model for filtering models. */
class ModelsFilter : Component() {

    val model = object : FilterSortModel<ModelInfo>() {
        init {
            addFilter("source") { it.source }
            addFilter("type") { it.type }
            addFilter("lifecycle") { it.lifecycle }
        }
    }

    /** Update and return the current filter. */
    fun filter(): (ModelInfo) -> Boolean {
        model.updateFilter()
        return model.filter.value!!
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
