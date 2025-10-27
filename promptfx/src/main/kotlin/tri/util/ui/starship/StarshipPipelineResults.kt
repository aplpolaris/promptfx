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
package tri.util.ui.starship

import com.fasterxml.jackson.databind.JsonNode
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import tornadofx.stringBinding

/** Object collecting observable results from a pipeline execution. */
class StarshipPipelineResults {

    /** Tracks results of execution by variable name. */
    private val vars = mutableMapOf<String, SimpleStringProperty>()
    /** Tracks multiple-choice options by variable name. */
    private val mcOptions = mutableMapOf<String, MultiChoiceObject>()

    /** Flag indicating start of execution. */
    val started = SimpleBooleanProperty(false)
    /** Active step (indicated by the variable being solved for). */
    val activeStepVar = SimpleObjectProperty<String>(null)
    /** Flag indicating completion. */
    val completed = SimpleBooleanProperty(false)

    /** Creates an observable value for tracking the value of a variable, if not already present. */
    fun observableFor(widget: StarshipConfigWidget) =
        vars.getOrPut(widget.varRef) { SimpleStringProperty(null) }

    /** Creates an observable value for tracking the value of a variable, allowing users to cycle between options. */
    fun actionFor(key: String, values: List<String>): Pair<ObservableValue<String>, () -> Unit> {
        val mc = mcOptions.getOrPut(key) { MultiChoiceObject(values) }
        return Pair(mc.value, { mc.next() })
    }

    /** Gets all current multiple-choice variable values. */
    fun getMultiChoiceValues(): Map<String, String> =
        mcOptions.mapValues { it.value.value.value }

    /** Updates the value of a variable. */
    fun updateVariable(key: String, value: JsonNode) {
        val prop = vars.getOrPut(key) { SimpleStringProperty(null) }
        prop.set(value.unwrappedTextValue())
    }

    /** Clear results, while keeping current values of multichoice options. */
    fun clearResults() {
        vars.values.forEach { it.set(null) }
        started.set(false)
        completed.set(false)
        activeStepVar.set(null)
    }
}

/** Manages multiple-choice options for a variable, with an action to cycle through options. */
class MultiChoiceObject(val options: List<String>) {
    private val index = SimpleIntegerProperty(0)
    val value = index.stringBinding { options[it!!.toInt()] }
    fun next() {
        index.set((index.get() + 1) % options.size)
    }
}
