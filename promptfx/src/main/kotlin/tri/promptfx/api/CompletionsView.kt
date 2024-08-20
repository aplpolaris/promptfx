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
package tri.promptfx.api

import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.trace.*
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
        val response = if (completionModel != null) {
            completionModel.complete(
                text = input.get(),
                tokens = common.maxTokens.value,
                temperature = common.temp.value,
                numResponses = common.numResponses.value
            )
        } else {
            val completion = CompletionRequest(
                model = ModelId(id),
                prompt = input.get(),
                temperature = common.temp.value,
                topP = common.topP.value,
                frequencyPenalty = common.freqPenalty.value,
                presencePenalty = common.presPenalty.value,
                maxTokens = common.maxTokens.value,
                n = common.numResponses.value
            )
            controller.openAiPlugin.client.completion(completion)
        }
        return response.asPipelineResult(
            promptInfo = AiPromptInfo(input.get()),
            modelInfo = AiPromptModelInfo(id, common.toModelParams())
        )
    }

}
