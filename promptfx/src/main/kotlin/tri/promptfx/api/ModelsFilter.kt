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
