package tri.promptfx.integration

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.task
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.AudioPanel

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
        WeatherAiTaskPlanner(completionEngine, embeddingService, it).execute(progress)
    }.planner

    private suspend fun userInput(): String {
        var text = input.get()
        audio.file.value?.let {
            text = controller.openAiPlugin.client.quickTranscribe(audioFile = it).value!!
            input.set(text)
        }
        return text
    }

}

class WeatherViewPlugin : NavigableWorkspaceViewImpl<WeatherView>("Integrations", "Weather", WeatherView::class)
