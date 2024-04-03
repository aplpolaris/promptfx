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

    companion object {
        private const val DEFAULT_TEMP = 1.0
        private const val DEFAULT_TOP_P = 1.0
        private const val DEFAULT_FREQ_PENALTY = 0.0
        private const val DEFAULT_PRES_PENALTY = 0.0
        private const val DEFAULT_MAX_TOKENS = 500
        private const val DEFAULT_STOP_SEQUENCES = ""
    }

    internal val temp = SimpleDoubleProperty(DEFAULT_TEMP)
    internal val topP = SimpleDoubleProperty(DEFAULT_TOP_P)
    internal val freqPenalty = SimpleDoubleProperty(DEFAULT_FREQ_PENALTY)
    internal val presPenalty = SimpleDoubleProperty(DEFAULT_PRES_PENALTY)

    internal val maxTokens = SimpleIntegerProperty(DEFAULT_MAX_TOKENS)
    internal val stopSequences = SimpleStringProperty(DEFAULT_STOP_SEQUENCES)

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
    fun toModelParams() = listOf(
        AiPromptModelInfo.TEMPERATURE to if (temp.value == DEFAULT_TEMP) null else temp.value,
        AiPromptModelInfo.TOP_P to if (topP.value == DEFAULT_TOP_P) null else topP.value,
        AiPromptModelInfo.FREQUENCY_PENALTY to if (freqPenalty.value == DEFAULT_FREQ_PENALTY) null else freqPenalty.value,
        AiPromptModelInfo.PRESENCE_PENALTY to if (presPenalty.value == DEFAULT_PRES_PENALTY) null else presPenalty.value,
        AiPromptModelInfo.MAX_TOKENS to if (maxTokens.value == DEFAULT_MAX_TOKENS) null else maxTokens.value,
        AiPromptModelInfo.STOP to if (stopSequences.value == DEFAULT_STOP_SEQUENCES) null else stopSequences.value
    ).filter { it.second != null }
    .toMap() as Map<String, Any>

    /** Import model parameters from a map. */
    fun importModelParams(modelParams: Map<String, Any>) {
        (modelParams[AiPromptModelInfo.TEMPERATURE] as? Double)?.let { temp.set(it) }
        (modelParams[AiPromptModelInfo.TOP_P] as? Double)?.let { topP.set(it) }
        (modelParams[AiPromptModelInfo.FREQUENCY_PENALTY] as? Double)?.let { freqPenalty.set(it) }
        (modelParams[AiPromptModelInfo.PRESENCE_PENALTY] as? Double)?.let { presPenalty.set(it) }
        (modelParams[AiPromptModelInfo.MAX_TOKENS] as? Int)?.let { maxTokens.set(it) }
        (modelParams[AiPromptModelInfo.STOP] as? String)?.let { stopSequences.set(it) }
    }

}
