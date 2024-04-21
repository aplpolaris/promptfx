package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.animation.Timeline
import kotlinx.coroutines.delay
import tornadofx.keyframe
import tornadofx.millis
import tornadofx.seconds
import tornadofx.timeline

/** An icon that blinks. */
class BlinkingIndicator(icon: FontAwesomeIcon): FontAwesomeIconView(icon) {

    private var indicatorTimeline: Timeline? = null
    var initialDelayMillis: Int = 0
    var blinkTimeMillis: Int = 1000
    var opacityRange = 0.1..1.0

    fun startBlinking() = blinkIndicator(true)
    fun stopBlinking() = blinkIndicator(false)

    private fun blinkIndicator(start: Boolean) {
        indicatorTimeline?.stop()
        if (start) {
            indicatorTimeline = timeline(play = false) {
                keyframe((blinkTimeMillis/2).millis) {
                    keyvalue(opacityProperty(), opacityRange.endInclusive)
                }
                keyframe(blinkTimeMillis.millis) {
                    keyvalue(opacityProperty(), opacityRange.start)
                }
                cycleCount = Timeline.INDEFINITE
                setOnFinished { opacity = opacityRange.endInclusive }
                playFrom(initialDelayMillis.seconds)
            }
        }
    }
}