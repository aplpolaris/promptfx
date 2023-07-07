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
package tri.promptfx

import javafx.beans.property.SimpleStringProperty
import tri.ai.openai.promptPlan

/** View that provides a single input box and combines that with a prompt from the prompt library. */
open class AiCompletionView(
    title: String,
    description: String,
    val promptId: String,
    val tokenLimit: Int,
    val stop: String? = null
) : AiPlanTaskView(title, description) {

    private val input = SimpleStringProperty("")

    init {
        addInputTextArea(input)
    }

    override fun plan() = completionEngine.promptPlan(promptId, input.get(), tokenLimit, stop)

}
