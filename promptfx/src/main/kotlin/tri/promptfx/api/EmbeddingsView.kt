/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
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

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.combobox
import tornadofx.field
import tri.ai.core.TextPlugin
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskResult
import tri.promptfx.AiTaskView

class EmbeddingsView : AiTaskView("Embeddings", "Enter text to calculate embedding (each line will be calculated separately).") {

    private val input = SimpleStringProperty("")
    private val model = SimpleObjectProperty(TextPlugin.embeddingModels()[0])

    init {
        addInputTextArea(input)
        parameters("Embeddings") {
            field("Model") {
                combobox(model, TextPlugin.embeddingModels())
            }
        }
        val outputEditor = outputPane.lookup(".text-area") as javafx.scene.control.TextArea
        outputEditor.isWrapText = false
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val inputs = input.get().split("\n").filter { it.isNotBlank() }
        return model.value.calculateEmbedding(inputs).let {
            it.joinToString("\n") { it.joinToString(",", prefix = "[", postfix = "]") { it.format(3) } }
        }.let {
            AiTaskResult.result(it, model.value.modelId).asPipelineResult()
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

}
