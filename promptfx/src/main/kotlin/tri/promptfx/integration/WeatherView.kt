/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.integration

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.task
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.AudioPanel

/** Plugin for the [WeatherView]. */
class WeatherViewPlugin : NavigableWorkspaceViewImpl<WeatherView>("Integrations", "Ask about Weather", type = WeatherView::class)

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
