/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.ModelInfo

/** View for displaying model information. */
class ModelInfoCard(val selectedModel: SimpleObjectProperty<ModelInfo>) : View() {
    override val root = vbox {
        vgrow = Priority.ALWAYS

        scrollpane {
            vgrow = Priority.ALWAYS

            form {
                visibleWhen { selectedModel.isNotNull }
                managedWhen { selectedModel.isNotNull }

                fieldset("Model Info") {
                    modelfield("Id") { it.id }
                    modelfield("Name") { it.name }
                    modelfield("Type", { listOf(graphic(it.type)) }) { it.type }
                    modelfield("Description") { it.description }
                    modelfield("Inputs", { graphics(it.inputs) }) { it.inputs }
                    modelfield("Outputs", { graphics(it.outputs) }) { it.outputs }
                }
                fieldset("Model Version") {
                    modelfield("Source") { it.source }
                    modelfield("Version") { it.version }
                    modelfield("Lifecycle") { it.lifecycle }
                    modelfield("Created") { it.created }
                    modelfield("Deprecation") { it.deprecation }
                }
                fieldset("Model Limits") {
                    modelfield("Input Token Limit") { it.inputTokenLimit }
                    modelfield("Output Token Limit") { it.outputTokenLimit }
                    modelfield("Total Token Limit") { it.totalTokenLimit }
                    modelfield("Output Dimension") { it.outputDimension }
                }
                fieldset("Other Properties") {
                    field("Params") {
                        text(selectedModel.stringBinding {
                            it?.params?.entries?.joinToString("\n") { (k, v) -> "$k: $v" }
                        }) {
                            wrappingWidth = 300.0
                        }
                    }
                }
            }
        }
    }

    fun Fieldset.modelfield(text: String, iconOp: (ModelInfo) -> List<Node> = { listOf() }, op: (ModelInfo) -> Any?) {
        field(text) {
            val prop = selectedModel.stringBinding { it?.let { op(it) }?.toString() }
            visibleWhen(prop.isNotBlank())
            managedWhen(prop.isNotBlank())
            hbox(5, Pos.CENTER_LEFT) {
                hbox(5, Pos.CENTER_LEFT) {
                    selectedModel.onChange {
                        children.setAll(it?.let { iconOp(it) } ?: listOf())
                    }
                    managedWhen(children.sizeProperty.greaterThan(0))
                }
                text(prop) {
                    wrappingWidth = 300.0
                }
            }
        }
    }
}
