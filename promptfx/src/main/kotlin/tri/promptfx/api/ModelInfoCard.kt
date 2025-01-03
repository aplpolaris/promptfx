package tri.promptfx.api

import javafx.beans.property.SimpleObjectProperty
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

                fun Fieldset.modelfield(text: String, op: (ModelInfo) -> Any?) {
                    field(text) {
                        val prop = selectedModel.stringBinding { it?.let { op(it) }?.toString() }
                        visibleWhen(prop.isNotBlank())
                        managedWhen(prop.isNotBlank())
                        text(prop) {
                            wrappingWidth = 300.0
                        }
                    }
                }

                fieldset("Model Info") {
                    modelfield("Id") { it.id }
                    modelfield("Name") { it.name }
                    modelfield("Type") { it.type }
                    modelfield("Description") { it.description }
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
}