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