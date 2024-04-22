package tri.util.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.observableListOf
import tornadofx.stringBinding
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.batch.AiPromptRunConfig

/** Object collecting observable results from a pipeline execution. */
class StarshipPipelineResults {
    /** The initial string input. */
    val input = SimpleStringProperty(null)
    /** The initial run config, including prompt and model info. */
    val runConfig = SimpleObjectProperty<AiPromptRunConfig>(null)
    /** The output trace for primary prompt. */
    val output = SimpleObjectProperty<StarshipInterimResult>(null)
    /** The output text from the primary trace. */
    val outputText = output.stringBinding { it?.rawText ?: "" }
    /** Secondary run configs for additional prompts based on the first output. */
    val secondaryRunConfigs = observableListOf<AiPromptRunConfig>()
    /** Secondary outputs from additional prompts. */
    val secondaryOutputs = observableListOf<StarshipInterimResult>()

    /** Thumbnails associated with output. */
    val thumbnails = observableListOf<DocumentThumbnail>()
    /** Flag indicating start of execution. */
    val started = SimpleBooleanProperty(false)
    /** Flag indicating completion. */
    val completed = SimpleBooleanProperty(false)

    fun clearAll() {
        input.set(null)
        runConfig.set(null)
        output.set(null)
        secondaryRunConfigs.clear()
        secondaryOutputs.clear()
        started.set(false)
        completed.set(false)
    }
}