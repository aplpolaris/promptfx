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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.promptfx.PromptFxWorkspace
import tri.util.ui.graphic

/** View showing all details of a prompt trace. */
class PromptTraceDetailsUi : Fragment("Prompt Trace") {

    var trace = SimpleObjectProperty<AiPromptTraceSupport<*>>()

    val prompt = SimpleStringProperty("")
    val promptParams = SimpleObjectProperty<Map<String, Any>>(null)
    val model = SimpleStringProperty("")
    val modelParams = SimpleObjectProperty<Map<String, Any>>(null)
    val exec = SimpleObjectProperty<AiExecInfo>(null)
    val result = SimpleStringProperty("")

    lateinit var paramsField: Fieldset

    fun setTrace(trace: AiPromptTraceSupport<*>) {
        this.trace.set(trace)
        prompt.value = trace.prompt?.prompt
        promptParams.value = trace.prompt?.promptParams
        model.value = trace.model?.modelId
        modelParams.value = trace.model?.modelParams
        exec.value = trace.exec
        result.value = trace.output?.outputs?.get(0)?.toString() ?: "No result"
    }

    override val root = vbox {
        toolbar {
            // add button to close dialog and open trace in template view
            button("Open in template view", graphic = FontAwesomeIcon.SEND.graphic) {
                enableWhen(trace.isNotNull)
                tooltip("Copy this prompt to the Prompt Template view under Tools and open that view.")
                action {
                    close()
                    (workspace as PromptFxWorkspace).launchTemplateView(trace.value!!)
                }
            }
        }
        scrollpane {
            prefViewportHeight = 800.0
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            isFitToWidth = true
            form {
                fieldset("Input") {
                    field("Model") {
                        text(model)
                    }
                    field("Model Params") {
                        text(modelParams.stringBinding { it.pretty() })
                    }
                    field("Prompt") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(prompt) {
                            wrappingWidth = 400.0
                        }
                    }
                }
                paramsField = fieldset("Prompt Parameters")
                fieldset("Result") {
                    field("Execution") {
                        text(exec.stringBinding { it.pretty() })
                    }
                    field("Result") {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(result) {
                            wrappingWidth = 400.0
                        }
                    }
                }
            }
            updateParamsField()
            promptParams.onChange { updateParamsField() }
        }
    }


    private fun updateParamsField() {
        with (paramsField) {
            children.removeAll(paramsField.children.drop(1))
            promptParams.value?.let { params ->
                params.entries.forEach { (k, v) ->
                    field(k) {
                        labelContainer.alignment = Pos.TOP_LEFT
                        text(v.truncated) {
                            tooltip(v.toString())
                            wrappingWidth = 400.0
                        }
                    }
                }
            }
        }
    }

    private fun Map<String, Any?>?.pretty() =
        this?.entries?.joinToString(", ") { (k, v) -> "$k: ${v.truncated}" } ?: ""
    private fun AiExecInfo?.pretty() = this?.let {
        mapOf<String, Any?>(
            "error" to it.error,
            "query_tokens" to it.queryTokens,
            "response_tokens" to it.responseTokens,
            "duration" to it.responseTimeMillis?.let { "${it}ms" }
        ).entries.filter { it.value != null }
            .joinToString(", ") { (k, v) -> "$k: $v" }
    }

    private val Any?.truncated
        get() = toString().let { if (it.length > 400) it.substring(0, 397) + "..." else it }

}
