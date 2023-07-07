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
package tri.promptfx.api

import com.aallam.openai.api.edits.EditsRequest
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.openai.editsModels
import tri.promptfx.AiTaskView
import tri.promptfx.CommonParameters

class EditsView : AiTaskView("Edit", "Enter text to edit, and instructions to apply to the text below.") {

    private val input = SimpleStringProperty("")
    private val instructText = SimpleStringProperty("")
    private val model = SimpleStringProperty(editsModels[0])
    private val length = SimpleIntegerProperty(50)
    private var common = CommonParameters()

    init {
        addInputTextArea(input)
        input {
            text("Instructions")
            textarea(instructText) {
                isWrapText = true
            }
        }
    }

    init {
        parameters("Edits") {
            field("Model") {
                combobox(model, editsModels)
            }
        }
        parameters("Parameters") {
            with(common) {
                temperature()
                topP()
            }
        }
        parameters("Output") {
            field("Maximum Length") {
                slider(0..500) {
                    valueProperty().bindBidirectional(length)
                }
                label(length.asString())
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val request = EditsRequest(
            model = ModelId(model.value),
            input = input.get(),
            instruction = instructText.get(),
            temperature = common.temp.value,
            topP = common.topP.value
        )
        return controller.openAiPlugin.client.edit(request).asPipelineResult()
    }

}
