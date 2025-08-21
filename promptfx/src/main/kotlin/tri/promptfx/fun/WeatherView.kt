package tri.promptfx.`fun`

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.task
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.AudioPanel

/** Plugin for the [WeatherView]. */
class WeatherViewPlugin : NavigableWorkspaceViewImpl<WeatherView>("Fun", "Ask about Weather", type = WeatherView::class)

/** View demonstrating integration of an external API and audio input. */
class WeatherView : AiPlanTaskView("Weather", "Enter a natural language query for weather information.") {

    private val input = SimpleStringProperty("")
    private val audio = AudioPanel().apply {
        fileText.onChange {
            input.set(it)
        }
    }

    init {
        input {
            audio.attachTo(this)
        }
        addInputTextArea(input)
    }

    override fun plan() = task("audio-transcribe") {
        userInput()
    }.task("weather") {
        WeatherAiTaskPlanner(completionEngine, embeddingModel, it).execute(progress)
    }.planner

    private suspend fun userInput(): String {
        var text = input.get()
        audio.file.value?.let {
            text = controller.openAiPlugin.client.quickTranscribe(audioFile = it).firstValue!!
            input.set(text)
        }
        return text
    }

}