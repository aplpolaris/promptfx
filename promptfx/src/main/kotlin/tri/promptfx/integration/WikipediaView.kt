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
package tri.promptfx.integration

import javafx.beans.property.SimpleStringProperty
import javafx.scene.text.Text
import tornadofx.*
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

class WikipediaView: AiPlanTaskView("Wikipedia", "Enter a question to ask Wikipedia.") {

    private val input = SimpleStringProperty("")
    private val pageTitle = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        inputPane.add(Text("Wikipedia Page Source:"))
        inputPane.add(textarea(pageTitle) {
            isWrapText = true
        })
    }

    override fun plan() = WikipediaAiTaskPlanner(completionEngine, pageTitle, input.get())

}

class WikipediaViewPlugin : NavigableWorkspaceViewImpl<WikipediaView>("Integrations", "Wikipedia", WikipediaView::class)
