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
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.pips.AiPipelineResult
import tri.promptfx.AiTaskView
import tri.promptfx.ModelParameters
import tri.util.ui.slider

class CompletionsView : AiTaskView("Completion", "Enter text to complete") {

    private val input = SimpleStringProperty("")
    private val model = SimpleObjectProperty(TextPlugin.textCompletionModels().first())
    private var common = ModelParameters()

    init {
        addInputTextArea(input)
        parameters("Completion Model") {
            field("Model") {
                combobox(model, TextPlugin.textCompletionModels())
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                topP()
                frequencyPenalty()
                presencePenalty()
                maxTokens()
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val completionModel = TextPlugin.textCompletionModels().firstOrNull { it.modelId == model.value.modelId }
        return if (completionModel != null) {
            completionModel.complete(
                text = input.get(),
                tokens = common.maxTokens.value
            ).asPipelineResult()
        } else {
            val completion = CompletionRequest(
                model = ModelId(model.value.modelId),
                prompt = input.get(),
                temperature = common.temp.value,
                topP = common.topP.value,
                frequencyPenalty = common.freqPenalty.value,
                presencePenalty = common.presPenalty.value,
                maxTokens = common.maxTokens.value,
            )
            controller.openAiPlugin.client.completion(completion).asPipelineResult()
        }
    }

}
