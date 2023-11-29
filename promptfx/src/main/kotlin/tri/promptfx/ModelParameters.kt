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
package tri.promptfx

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import tornadofx.field
import tornadofx.label
import tornadofx.tooltip
import tri.util.ui.slider

/** Parameters for model content generation. */
class ModelParameters {

    internal val temp = SimpleDoubleProperty(1.0)
    internal val topP = SimpleDoubleProperty(1.0)
    internal val freqPenalty = SimpleDoubleProperty(0.0)
    internal val presPenalty = SimpleDoubleProperty(0.0)

    internal val maxTokens = SimpleIntegerProperty(500)

    fun EventTarget.temperature() {
        field("Temperature") {
            slider(0.0..2.0, temp)
            label(temp.asString("%.2f"))
        }
    }

    fun EventTarget.topP() {
        field("Top P") {
            slider(0.0..1.0, topP)
            label(topP.asString("%.2f"))
        }
    }

    fun EventTarget.frequencyPenalty() {
        field("Frequency Penalty") {
            slider(-2.0..2.0, freqPenalty)
            label(freqPenalty.asString("%.2f"))
        }
    }

    fun EventTarget.presencePenalty() {
        field("Presence Penalty") {
            slider(-2.0..2.0, presPenalty)
            label(presPenalty.asString("%.2f"))
        }
    }

    fun EventTarget.maxTokens() {
        field("Maximum Tokens") {
            tooltip("Max # of tokens for combined query/response from the text completion engine")
            slider(0..2000, maxTokens)
            label(maxTokens.asString())
        }
    }

}
