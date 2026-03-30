/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2026 Johns Hopkins University Applied Physics Laboratory
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
package tri.util.ui.starship

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import tornadofx.*

/** Strip height in logical pixels (the design space is 1920×1080). */
private const val STRIP_HEIGHT = 48.0
/** Vertical margin from the bottom of the 1920×1080 canvas. */
private const val STRIP_BOTTOM_MARGIN = 20.0
/** Horizontal padding inside the strip. */
private const val STRIP_H_PADDING = 24.0
/** Size of state icon glyphs. */
private const val ICON_SIZE = 14.0

/** Orange accent colour used throughout StarshipView. */
private const val COLOR_ACCENT = "#EE8F33"
/** Muted text colour for pending/inactive steps. */
private const val COLOR_MUTED = "#777"
/** Failure colour. */
private const val COLOR_FAILURE = "#cc3333"

/**
 * Adds a persistent horizontal pipeline-progress strip to this [Pane].
 *
 * The strip sits near the bottom of the 1920×1080 canvas and shows every pipeline step by name
 * with a live state indicator:
 *  - **Pending** — gray circle
 *  - **Active** — pulsing orange circle
 *  - **Done** — orange check-mark
 *  - **Failed** — red × mark
 */
fun Pane.addPipelineProgressStrip(results: StarshipPipelineResults, screenWidth: Double, screenHeight: Double) {
    val stripY = screenHeight - STRIP_HEIGHT - STRIP_BOTTOM_MARGIN

    val container = hbox(0.0) {
        id = "starship-progress-strip"
        resizeRelocate(0.0, stripY, screenWidth, STRIP_HEIGHT)
        alignment = Pos.CENTER
        isMouseTransparent = true
        style = "-fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 8; -fx-padding: 0 ${STRIP_H_PADDING} 0 ${STRIP_H_PADDING};"
    }

    fun rebuild() {
        Platform.runLater {
            container.children.clear()
            val steps = results.stepStates.toList()
            steps.forEachIndexed { index, entry ->
                if (index > 0) {
                    container.label("›") {
                        style = "-fx-text-fill: #444; -fx-font-size: 18px; -fx-padding: 0 6 0 6;"
                    }
                }
                container.add(StepIndicatorItem(entry))
            }
        }
    }

    results.stepStates.addListener(ListChangeListener { rebuild() })
    rebuild()
}

/** A single step indicator node: [icon] + [label]. Reacts to state changes in [entry]. */
private class StepIndicatorItem(private val entry: PipelineStepEntry) : HBox(6.0) {

    private val icon = FontAwesomeIconView(FontAwesomeIcon.CIRCLE).apply {
        glyphSize = ICON_SIZE
        glyphStyle = "-fx-fill: $COLOR_MUTED;"
        opacity = 0.5
    }
    private val nameLabel = javafx.scene.control.Label(entry.name).apply {
        style = "-fx-text-fill: $COLOR_MUTED; -fx-font-size: 13px;"
    }
    private var pulseTimeline: Timeline? = null

    init {
        alignment = Pos.CENTER_LEFT
        isMouseTransparent = true
        children.addAll(icon, nameLabel)
        // Apply the current state immediately (in case initSteps was already called)
        applyState(entry.state.value)
        // React to future state changes
        entry.state.addListener { _, _, newState -> Platform.runLater { applyState(newState) } }
    }

    private fun applyState(state: PipelineStepState) {
        pulseTimeline?.stop()
        pulseTimeline = null
        icon.opacity = 1.0

        when (state) {
            PipelineStepState.PENDING -> {
                icon.setIcon(FontAwesomeIcon.CIRCLE)
                icon.glyphStyle = "-fx-fill: $COLOR_MUTED;"
                icon.opacity = 0.5
                nameLabel.style = "-fx-text-fill: $COLOR_MUTED; -fx-font-size: 13px;"
            }
            PipelineStepState.ACTIVE -> {
                icon.setIcon(FontAwesomeIcon.CIRCLE)
                icon.glyphStyle = "-fx-fill: $COLOR_ACCENT;"
                nameLabel.style = "-fx-text-fill: $COLOR_ACCENT; -fx-font-size: 13px; -fx-font-weight: bold;"
                // Pulse the icon opacity between 0.3 and 1.0 to signal activity
                pulseTimeline = timeline(play = true) {
                    keyframe(0.millis) { keyvalue(icon.opacityProperty(), 1.0) }
                    keyframe(500.millis) { keyvalue(icon.opacityProperty(), 0.3) }
                    keyframe(1000.millis) { keyvalue(icon.opacityProperty(), 1.0) }
                    cycleCount = Timeline.INDEFINITE
                }
            }
            PipelineStepState.DONE -> {
                icon.setIcon(FontAwesomeIcon.CHECK)
                icon.glyphStyle = "-fx-fill: $COLOR_ACCENT;"
                nameLabel.style = "-fx-text-fill: $COLOR_ACCENT; -fx-font-size: 13px;"
            }
            PipelineStepState.FAILED -> {
                icon.setIcon(FontAwesomeIcon.TIMES)
                icon.glyphStyle = "-fx-fill: $COLOR_FAILURE;"
                nameLabel.style = "-fx-text-fill: $COLOR_FAILURE; -fx-font-size: 13px;"
            }
        }
    }
}
