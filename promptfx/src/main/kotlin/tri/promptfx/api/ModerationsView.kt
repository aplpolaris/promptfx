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

import com.aallam.openai.api.moderation.ModerationModel
import com.aallam.openai.api.moderation.ModerationRequest
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.util.json.jsonMapper
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.asPipelineResult
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.AiTaskView

/** View for text moderation API. */
class ModerationsView : AiTaskView("Moderations", "Enter text to generate moderation scores") {

    private val input = SimpleStringProperty("")
    private val model = SimpleObjectProperty(ModerationModel.Latest)

    init {
        addInputTextArea(input)
        parameters("Moderations") {
            field("Model") {
                combobox(model, listOf(ModerationModel.Latest, ModerationModel.Stable)) {
                    cellFormat { text = it.model }
                }
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val request = ModerationRequest(
            input = listOf(input.value),
            model = model.value
        )
        val response = controller.openAiPlugin.client.client.moderations(request)
        val responseText = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
        return AiPromptTrace(
            null,
            AiModelInfo(model.value.model),
            AiExecInfo(),
            AiOutputInfo.text(responseText)
        ).asPipelineResult()
    }

}
