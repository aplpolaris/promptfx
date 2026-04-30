/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.stringBinding
import tri.ai.pips.api.PPlanStep

/** Possible states for a pipeline step in the progress strip. */
enum class PipelineStepState { PENDING, ACTIVE, DONE, FAILED }

/** An observable entry for a single pipeline step shown in the progress strip. */
class PipelineStepEntry(
    /** Display name derived from the step's description, saveAs variable, or tool name. */
    val name: String
) {
    /** Current execution state of this step. */
    val state: SimpleObjectProperty<PipelineStepState> = SimpleObjectProperty(PipelineStepState.PENDING)
}

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

    /** Observable list of pipeline steps with their current states, used by the progress strip. */
    val stepStates: ObservableList<PipelineStepEntry> = FXCollections.observableArrayList()

    /**
     * Initialises the step list from the pipeline plan. Must be called at the start of each execution.
     * Safe to call from a background thread.
     */
    fun initSteps(steps: List<PPlanStep>) {
        Platform.runLater {
            stepStates.setAll(steps.map { PipelineStepEntry(it.description ?: it.saveAs ?: it.tool) })
        }
    }

    /**
     * Updates the execution state for the step at [index].
     * Safe to call from a background thread.
     */
    fun setStepState(index: Int, state: PipelineStepState) {
        Platform.runLater {
            if (index in stepStates.indices) stepStates[index].state.set(state)
        }
    }

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
    fun updateVariable(key: String, value: Any?) {
        val prop = vars.getOrPut(key) { SimpleStringProperty(null) }
        prop.set(valueToDisplayString(value))
    }

    companion object {
        /** Converts a scratchpad value to a displayable string for UI presentation. */
        internal fun valueToDisplayString(value: Any?): String? = when (value) {
            null -> null
            is JsonNode -> value.unwrappedTextValue()
            else -> value.toString()
        }
    }

    /** Clear results, while keeping current values of multichoice options. */
    fun clearResults() {
        vars.values.forEach { it.set(null) }
        started.set(false)
        completed.set(false)
        activeStepVar.set(null)
        stepStates.forEach { it.state.set(PipelineStepState.PENDING) }
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
