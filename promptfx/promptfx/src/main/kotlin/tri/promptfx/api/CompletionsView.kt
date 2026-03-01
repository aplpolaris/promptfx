/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.api

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.asPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.ModelParameters
import tri.promptfx.PromptFxModels

/** View for text completion API. */
class CompletionsView : AiTaskView("Completion", "Enter text to complete") {

    private val input = SimpleStringProperty("")
    private val model = SimpleObjectProperty(PromptFxModels.textCompletionModelDefault())
    private var common = ModelParameters()

    init {
        addInputTextArea(input)
        parameters("Completion Model") {
            field("Model") {
                combobox(model, PromptFxModels.textCompletionModels())
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                topP()
                frequencyPenalty()
                presencePenalty()
                maxTokens()
                numResponses()
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val id = model.value!!.modelId
        val completionModel = PromptFxModels.textCompletionModels().firstOrNull { it.modelId == id }
            ?: throw UnsupportedOperationException("Model not found: $id")
        return common.completionBuilder()
            .text(input.get())
            .execute(completionModel)
            .asPipelineResult()
    }

}
