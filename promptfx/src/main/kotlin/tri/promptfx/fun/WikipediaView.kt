/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.promptfx.`fun`

import javafx.beans.property.SimpleStringProperty
import tornadofx.text
import tornadofx.toolbar
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [WikipediaView]. */
class WikipediaViewPlugin : NavigableWorkspaceViewImpl<WikipediaView>("Fun", "Wikipedia Q&A", type = WikipediaView::class)

/** View to answer questions using wikipedia. */
class WikipediaView: AiPlanTaskView("Wikipedia", "Enter a question to ask Wikipedia.") {

    private val input = SimpleStringProperty("")
    private val pageTitle = SimpleStringProperty("")

    init {
        addInputTextArea(input)
        input {
            toolbar {
                text("Wikipedia Page Source:")
            }
        }
        addInputTextArea(pageTitle)
    }

    override fun plan() = WikipediaAiTaskPlanner(chatEngine, common, pageTitle, input.get())

}
