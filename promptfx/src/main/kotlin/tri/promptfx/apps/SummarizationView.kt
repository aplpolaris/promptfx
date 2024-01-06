/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
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
package tri.promptfx.apps

import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.openai.templatePlan
import tri.ai.pips.AiPlanner
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

/** Plugin for [SummarizationView]. */
class SummarizationPlugin : NavigableWorkspaceViewImpl<SummarizationView>("Text", "Summarization", SummarizationView::class)

/** A view that allows the user to summarize text. */
class SummarizationView: AiPlanTaskView("Summarization", "Enter text to summarize") {

    private val res = resources.yaml("resources/modes.yaml")
    private val audienceOptions = res["summarization-audience"] as Map<String, String>
    private val styleOptions = res["summarization-style"] as Map<String, String>
    private val outputOptions = res["summarization-format"] as Map<String, String>

    private val sourceText = SimpleStringProperty("")
    private val modeAudience = SimpleStringProperty(audienceOptions.keys.first())
    private val modeStyle = SimpleStringProperty(styleOptions.keys.first())
    private val modeOutput = SimpleStringProperty(outputOptions.keys.first())

    init {
        addInputTextArea(sourceText)
        parameters("Summarization Options") {
            field("Summarize for") {
                combobox(modeAudience, audienceOptions.keys.toList())
            }
            field("Style of") {
                combobox(modeStyle, styleOptions.keys.toList())
            }
            field("Shown as") {
                combobox(modeOutput, outputOptions.keys.toList())
            }
        }
        parameters("Model Parameters") {
            with (common) {
                temperature()
                maxTokens()
            }
        }
    }

    override fun plan(): AiPlanner {
        val instruct = listOf(audienceOptions[modeAudience.value]!!,
            styleOptions[modeStyle.value]!!,
            outputOptions[modeOutput.value]!!)
            .filter { it.isNotBlank() }
            .joinToString(" and ")
        val fullInstruct = if (instruct.isBlank()) "" else "The result should be $instruct."
        return completionEngine.templatePlan("summarization",
            "instruct" to fullInstruct,
            "input" to sourceText.get(),
            tokenLimit = common.maxTokens.value!!,
            temp = common.temp.value,
        )
    }

}
