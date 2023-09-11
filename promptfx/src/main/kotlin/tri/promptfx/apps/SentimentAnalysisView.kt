/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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
import tornadofx.*
import tri.ai.openai.instructTextPlan
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.yaml

/** Plugin for the [SentimentAnalysisView]. */
class SentimentAnalysisPlugin : NavigableWorkspaceViewImpl<SentimentAnalysisView>("Text", "Sentiment Analysis", SentimentAnalysisView::class)

/** View designed to classify the sentiment of a text. */
class SentimentAnalysisView: AiPlanTaskView("Sentiment Analysis",
    "Enter text to determine sentiment (or provide a numbered list of items)") {

    private val modeOptions = resources.yaml("resources/modes.yaml")["sentiment"] as List<String>
    private val sourceText = SimpleStringProperty("")
    private val mode = SimpleStringProperty(modeOptions[0])

    init {
        addInputTextArea(sourceText)
        parameters("Sentiment Mode") {
            field("Mode") {
                combobox(mode, modeOptions)
            }
        }
    }

    override fun plan() = completionEngine.instructTextPlan("sentiment-classify",
        instruct = mode.get(),
        userText = sourceText.get(),
        tokenLimit = 500)

}
