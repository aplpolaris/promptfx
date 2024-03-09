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

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import tri.ai.prompt.trace.AiPromptExecInfo
import tri.ai.prompt.trace.AiPromptTrace

/** A card that displays the trace of a prompt. */
class PromptTraceCard : Fragment() {

    val result = SimpleStringProperty("")

    private var trace: AiPromptTrace? = null

    fun setTrace(trace: AiPromptTrace) {
        result.value = trace.outputInfo.output
        this.trace = trace
    }

    override val root = vbox {
        label(result)

        onLeftClick {
            trace?.let {
                find<PromptTraceDetails>().apply {
                    setTrace(it)
                    openModal()
                }
            }
        }
    }
}

class PromptTraceDetails : Fragment("Prompt Trace") {

    val prompt = SimpleStringProperty("")
    val promptParams = SimpleObjectProperty<Map<String, Any>>(null)
    val model = SimpleStringProperty("")
    val modelParams = SimpleObjectProperty<Map<String, Any>>(null)
    val exec = SimpleObjectProperty<AiPromptExecInfo>(null)
    val result = SimpleStringProperty("")

    fun setTrace(trace: AiPromptTrace) {
        prompt.value = trace.promptInfo.prompt
        promptParams.value = trace.promptInfo.promptParams
        model.value = trace.modelInfo.modelId
        modelParams.value = trace.modelInfo.modelParams
        exec.value = trace.execInfo
        result.value = trace.outputInfo.output
    }

    override val root = vbox {
        form {
            fieldset("Input") {
                field("Prompt") { label(prompt) }
                field("Prompt Params") { label(promptParams.stringBinding { it.prettyPerLine() }) }
                field("Model") { label(model) }
                field("Model Params") { label(modelParams.stringBinding { it.pretty() }) }
            }
            fieldset("Result") {
                field("Execution") { label(exec.stringBinding { it.pretty() }) }
                field("Result") { label(result) }
            }
        }
    }

    private fun Map<String, Any?>?.prettyPerLine() = this?.entries?.joinToString("\n") { (k, v) -> "$k: $v" } ?: ""
    private fun Map<String, Any?>?.pretty() = this?.entries?.joinToString(", ") { (k, v) -> "$k: $v" } ?: ""
    private fun AiPromptExecInfo?.pretty() = this?.let {
        mapOf<String, Any?>(
            "error" to it.error,
            "query_tokens" to it.queryTokens,
            "response_tokens" to it.responseTokens,
            "duration" to "${it.responseTimeMillis}ms"
        ).entries.filter { it.value != null }
            .joinToString(", ") { (k, v) -> "$k: $v" }
    }

}
