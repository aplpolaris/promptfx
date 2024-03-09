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
package tri.promptfx.ui

import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace

/** A card that displays the trace of a prompt. */
class PromptTraceCard : Fragment() {

    val prompt = SimpleStringProperty("")
    val promptParams = SimpleStringProperty("")
    val model = SimpleStringProperty("")
    val modelParams = SimpleStringProperty("")
    val exec = SimpleStringProperty("")
    val result = SimpleStringProperty("")

    fun setTrace(trace: AiPromptTrace) {
        prompt.value = trace.promptInfo.prompt
        promptParams.value = trace.promptInfo.promptParams.toString()
        model.value = trace.modelInfo.modelId
        modelParams.value = trace.modelInfo.modelParams.toString()
        exec.value = trace.execInfo.toString()
        result.value = trace.outputInfo.output
    }

    override val root = form {
        fieldset("Input") {
            field("Prompt") { textfield(prompt) }
            field("Prompt Params") { textfield(promptParams) }
            field("Model") { textfield(model) }
            field("Model Params") { textfield(modelParams) }
        }
        fieldset("Result") {
            field("Execution") { textfield(exec) }
            field("Result") { textarea(result) }
        }
    }
}
