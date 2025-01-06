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
                keyframe(0.millis) { keyvalue(opacityProperty(), opacityRange.endInclusive) }
                keyframe((0.5*blinkTimeMillis).millis) { keyvalue(opacityProperty(), opacityRange.start) }
                keyframe(blinkTimeMillis.millis) { keyvalue(opacityProperty(), opacityRange.endInclusive) }
                cycleCount = Timeline.INDEFINITE
                setOnFinished { opacity = opacityRange.endInclusive }
                playFrom(initialDelayMillis.seconds)
            }
        }
    }
}
