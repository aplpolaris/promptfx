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
package tri.promptfx

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import tornadofx.field
import tornadofx.label
import tornadofx.textfield
import tornadofx.tooltip
import tri.ai.prompt.trace.AiPromptModelInfo
import tri.util.ui.slider

/** Parameters for model content generation. */
class ModelParameters {

    internal val temp = SimpleDoubleProperty(1.0)
    internal val topP = SimpleDoubleProperty(1.0)
    internal val freqPenalty = SimpleDoubleProperty(0.0)
    internal val presPenalty = SimpleDoubleProperty(0.0)

    internal val maxTokens = SimpleIntegerProperty(500)
    internal val stopSequences = SimpleStringProperty("")

    fun EventTarget.temperature() {
        field("Temperature") {
            tooltip("Controls the randomness of the generated text. Lower values make the text more deterministic, higher values make it more random.")
            slider(0.0..2.0, temp)
            label(temp.asString("%.2f"))
        }
    }

    fun EventTarget.topP() {
        field("Top P") {
            tooltip("Controls the diversity of the generated text. Lower values make the text more deterministic, higher values make it more random.")
            slider(0.0..1.0, topP)
            label(topP.asString("%.2f"))
        }
    }

    fun EventTarget.frequencyPenalty() {
        field("Frequency Penalty") {
            tooltip("Penalizes new tokens based on existing frequency in the text so far, decreasing likelihood of repetition (or increasing if the value is negative).")
            slider(-2.0..2.0, freqPenalty)
            label(freqPenalty.asString("%.2f"))
        }
    }

    fun EventTarget.presencePenalty() {
        field("Presence Penalty") {
            tooltip("Penalizes new tokens based on existing presence in the text so far, increasing likelihood of new topics (or decreasing if the value is negative).")
            slider(-2.0..2.0, presPenalty)
            label(presPenalty.asString("%.2f"))
        }
    }

    fun EventTarget.maxTokens() {
        field("Maximum Tokens") {
            tooltip("Maximum number of tokens for combined query and response from the model (interpretation may vary by model).")
            slider(0..2000, maxTokens)
            label(maxTokens.asString())
        }
    }

    fun EventTarget.stopSequences() {
        field("Stop Sequences") {
            tooltip("A list of up to 4 sequences where the API will stop generating further tokens. Use || to separate sequences.")
            textfield(stopSequences)
        }
    }

    /** Generate model parameters object for [AiPromptModelInfo]. */
    fun toModelParams() = mapOf(
        AiPromptModelInfo.MAX_TOKENS to maxTokens.value,
        AiPromptModelInfo.TEMPERATURE to temp.value
    ) + if (stopSequences.value.isBlank()) emptyMap() else mapOf(
        AiPromptModelInfo.STOP to stopSequences.value
    )

}
