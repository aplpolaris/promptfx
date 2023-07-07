package tri.promptfx

import javafx.beans.property.SimpleDoubleProperty
import javafx.event.EventTarget
import tornadofx.*
import tri.util.ui.slider

class CommonParameters {

    internal val temp = SimpleDoubleProperty(1.0)
    internal val topP = SimpleDoubleProperty(1.0)
    internal val freqPenalty = SimpleDoubleProperty(0.0)
    internal val presPenalty = SimpleDoubleProperty(0.0)

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

}