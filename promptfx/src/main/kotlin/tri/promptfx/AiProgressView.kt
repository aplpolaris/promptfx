package tri.promptfx

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.layout.Priority
import javafx.stage.Popup
import tornadofx.*
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskMonitor
import tri.ai.pips.PrintMonitor

/** Global progress bar. */
class AiProgressView: View(), AiTaskMonitor {

    lateinit var indicator: javafx.scene.control.ProgressBar
    lateinit var label: Label

    var task: Task<*>? = null
        set(value) {
            field = value
            fxTask = task as? FXTask<*>
        }
    var fxTask: FXTask<*>? = null

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

    //region MONITOR

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
        Platform.runLater {
            indicator.progress = progress
            label.text = task.id
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

    private fun end() {
        Platform.runLater {
            indicator.isVisible = false
            label.isVisible = false
            indicator.progress = -1.0
        }
    }

    //endregion

}