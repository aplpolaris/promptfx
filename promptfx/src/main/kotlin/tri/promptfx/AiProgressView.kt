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
package tri.promptfx

import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.concurrent.Task
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.layout.Priority
import javafx.stage.Popup
import tornadofx.*
import tri.ai.pips.*
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport

/** Global progress bar. */
class AiProgressView: View(), AiTaskMonitor {

    lateinit var indicator: javafx.scene.control.ProgressBar
    lateinit var label: Label

    override val root = borderpane {
        hgrow = Priority.ALWAYS
        padding = insets(5.0)
        center = progressbar {
            indicator = this
            hgrow = Priority.ALWAYS
            prefWidthProperty().bind(this@borderpane.widthProperty())
            style { fontSize = 22.px }
            setOnMouseClicked { e ->
                if (e.button === MouseButton.PRIMARY) {
                    Popup().apply {
                        show(this@progressbar, e.screenX, e.screenY)
                    }
                }
            }
        }
        right = label {
            label = this
            padding = insets(5.0)
            style { fontSize = 22.px }
        }

        indicator.isVisible = false
        label.isVisible = false
    }

    /** Observable value for the active/visible state of the progress bar. */
    val activeProperty: BooleanProperty = indicator.visibleProperty()

    init {
        indicator.managedProperty().bind(indicator.visibleProperty())
        label.managedProperty().bind(label.visibleProperty())
    }

    //region MONITOR

    /** Start progress bar for a given task. */
    fun taskStarted(id: String) {
        taskStarted(task(id) { "" }.lastTask)
    }

    /** End all tasks and hide progress bar. */
    fun taskCompleted() {
        end()
    }

    override fun taskStarted(task: AiTask<*>) {
        PrintMonitor().taskStarted(task)
        Platform.runLater {
            indicator.progress = -1.0
            indicator.isVisible = true
            label.isVisible = true
            label.text = task.id
        }
    }

    override fun taskUpdate(task: AiTask<*>, progress: Double) {
        PrintMonitor().taskUpdate(task, progress)
        progressUpdate(task.id, progress)
    }

    fun progressUpdate(message: String, progress: Double) {
        Platform.runLater {
            indicator.progress = progress
            label.text = message
        }
    }

    override fun taskCompleted(task: AiTask<*>, result: Any?) {
        PrintMonitor().taskCompleted(task, result)
        end()
    }

    override fun taskFailed(task: AiTask<*>, error: Throwable) {
        PrintMonitor().taskFailed(task, error)
        end()
    }

    fun taskStarted(task: Task<AiPipelineResult<*>>, id: String) {
        taskStarted(object : AiTask<Any>(id) {
            override suspend fun execute(
                inputs: Map<String, AiPromptTraceSupport<*>>,
                monitor: AiTaskMonitor
            ) = task.value as AiPromptTrace<Any>
        })
    }

    /** Hook for printing completion of simple tasks. */
    fun taskCompleted(id: String) {
        PrintMonitor().taskCompleted(id)
        end()
    }

    private fun end() {
        Platform.runLater {
            indicator.isVisible = false
            label.isVisible = false
            indicator.progress = -1.0
        }
    }

    //endregion

}
