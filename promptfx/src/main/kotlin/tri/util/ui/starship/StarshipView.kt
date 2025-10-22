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
package tri.util.ui.starship

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.animation.Timeline
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.geometry.BoundingBox
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.transform.Rotate
import javafx.stage.Screen
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.promptfx.PromptFxController
import tri.promptfx.docs.DocumentQaView
import tri.ai.text.docs.FormattedText
import tri.promptfx.ui.ImmersiveChatView
import tri.promptfx.ui.toFxNodes
import tri.util.info
import tri.util.ui.*
import tri.util.ui.AnimatingThumbnailBox
import tri.util.ui.starship.Chromify.chromify

/** View for a full-screen animated text display. */
class StarshipView : Fragment("Starship") {

    val baseComponentTitle: String? by param()
    val baseComponent: View? by param()

    val controller: PromptFxController by inject()

    val configs = StarshipConfig()
    val layout
        get() = configs.layout
    val pipeline
        get() = configs.pipeline
    val results = StarshipPipelineResults()

    private val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
        ?: Screen.getPrimary()
    private val screenWidth = 1920 // curScreen.bounds.width
    private val screenHeight = 1080 // curScreen.bounds.height

    private val css1 = ImmersiveChatView::class.java.getResource("resources/chat.css")!!
    private val css2 = StarshipView::class.java.getResource("resources/starship.css")!!

    private var isExplainerVisible = false

    //region DERIVED PROPERTIES

    private val uw
        get() = screenWidth / layout.numCols.toDouble()
    private val uh
        get() = screenHeight / layout.numRows.toDouble()

    private val INSETS = 60.0

    private fun StarshipConfigWidget.px() =
        INSETS + uw * (pos.x - 1).toDouble()
    private fun StarshipConfigWidget.py() =
        INSETS + uh * (pos.y - 1).toDouble()
    private fun StarshipConfigWidget.pw() =
        uw * pos.width.toDouble() - 2 * INSETS
    private fun StarshipConfigWidget.ph() =
        uh * pos.height.toDouble() - 2 * INSETS

    //endregion

    override val root = pane {
        stylesheets.add(css1.toExternalForm())
        stylesheets.add(css2.toExternalForm())

        if (layout.isShowGrid) {
            addGrid()
        }

        val chromePane = pane {
            resizeRelocate(0.0, 0.0, 5.0, 5.0)
        }

        // animating text widgets - positioned absolutely as described in config
        layout.widgets.filter { it.widgetType == StarshipWidgetType.ANIMATING_TEXT }.forEach { widget ->
            add(createWidget(widget, chromePane))
        }

        // vertical text widgets - grouped by location so they can be stacked
        layout.widgets.filter { it.widgetType == StarshipWidgetType.ANIMATING_TEXT_VERTICAL }
            .groupBy { it.pos.x to it.pos.y }
            .forEach { (loc, widgets) ->
                vbox(54.0) {
                    val w = widgets.first()
                    resizeRelocate(w.px(), w.py(), w.pw(), w.ph())
                    // TODO - should we bind to results instead so we get intermediate processing updates dynamically (and don't show all chrome all the time?)
                    // TODO - can we reuse AnimatingTextWidget as-is, or do we need to tweak it?
                    bindChildren(widgets.asObservable()) { w ->
                        createWidget(w, chromePane).root
                    }
                }
            }

        // thumbnail widgets - positioned absolutely as described in config
        layout.widgets.filter { it.widgetType == StarshipWidgetType.ANIMATING_THUMBNAILS }.forEach { widget ->
            // TODO - fix hardcoded link to results thumbnails
            add(ThumbnailWidget(results.thumbnails).view)
        }

        // fill background with 100 twinkling stars of various sizes
        addBackgroundIcons()
        // add rocket indicator in lower-right corner to indicate ongoing processing
        addProgressIndicator()
        // add explainer overlay that can be turned on/off with "X" keypress
        addExplainerOverlay()
    }

    //region RENDER HELPERS

    /** Overlays a grid on top of the view. */
    private fun Pane.addGrid() {
        (0 until layout.numCols).forEach { x ->
            (0 until layout.numRows).forEach { y ->
                rectangle(screenWidth * x / layout.numCols, screenHeight * y / layout.numRows, screenWidth / layout.numCols, screenHeight / layout.numRows) {
                    style = "-fx-stroke:black;-fx-fill:none"
                }
            }
        }
    }

    /** Adds a background pane with lots of semi-transparent icons for decoration. */
    private fun Pane.addBackgroundIcons() {
        pane {
            isMouseTransparent = true
            for (i in 0 until layout.backgroundIconCount) {
                val star = layout.backgroundIcon
                val size = (5..20).random()
                add(BlinkingIndicator(star).apply {
                    layoutX = (size..(screenWidth - size)).random().toDouble()
                    layoutY = (size..(screenHeight - size)).random().toDouble() + size
                    opacity = (5..30).random().toDouble() / 100
                    glyphSize = size
                    glyphStyle = "-fx-fill:gray;"
                    initialDelayMillis = (500..1500).random()
                    blinkTimeMillis = 5000
                    opacityRange = (0.5 * opacity)..minOf(1.0, 1.5 * opacity)
                })
            }
        }
    }

    /** Adds a blinker indicator to indicate processing. */
    private fun Pane.addProgressIndicator() {
        val indicator = BlinkingIndicator(FontAwesomeIcon.ROCKET).apply {
            glyphSize = 60.0
            glyphStyle = "-fx-fill:gray;"
        }
        add(indicator.apply {
            id = "starship-indicator"
            layoutX = screenWidth - glyphSize.toDouble() - 20.0
            layoutY = screenHeight - 20.0
            results.started.onChange { if (it) startBlinking() }
            results.completed.onChange { stopBlinking() }
        })
    }

    /** Adds a widget to the pane, with chromed decoration. */
    private fun createWidget(widget: StarshipConfigWidget, chromePane: Pane): Fragment {
        val observable = results.observableFor(widget)
        val buttonEntry = widget.overlay.options.entries.firstOrNull()
        if (buttonEntry == null) {
            return AnimatingTextWidget(widget, chromePane, observable).view
        } else {
            val (buttonText, buttonAction) = results.actionFor(buttonEntry.key, buttonEntry.value)
            return AnimatingTextWidget(widget, chromePane, observable, buttonText, buttonAction).view
        }
    }

    /** Adds an explainer overlay that can be toggled with "X" keypress. */
    private fun Pane.addExplainerOverlay() {
        val explainer = pane {
            isMouseTransparent = true
            isVisible = false
            layout.widgets.forEach { explainerOverlay(it) }
        }
        onKeyPressed = EventHandler { event ->
            if (event.code == KeyCode.X) {
                explainer.isVisible = !explainer.isVisible
                isExplainerVisible = explainer.isVisible
            }
        }
    }

    /** Add a single explainer overlay for the given widget. */
    private fun Pane.explainerOverlay(widget: StarshipConfigWidget) {
        // blink all pane content for 2 seconds whenever results activeStep property changes to step
        var blinker: Timeline? = null
        val initialDelayMillis = 0
        val blinkTimeMillis = 1000
        val opacityRange = 0.4..1.0
        val opacityInitial = 0.1

        pane {
            opacity = opacityInitial
            results.activeStep.onChange {
                if (it == 0) {
                    opacity = opacityInitial
                } else if (it == widget.overlay.step) {
                    // create timeline to blink opacity of this pane
                    blinker?.stop()
                    blinker = timeline(play = false) {
                        keyframe(0.millis) { keyvalue(opacityProperty(), opacityRange.endInclusive) }
                        keyframe((0.5*blinkTimeMillis).millis) { keyvalue(opacityProperty(), opacityRange.start) }
                        keyframe(blinkTimeMillis.millis) { keyvalue(opacityProperty(), opacityRange.endInclusive) }
                        cycleCount = 5
                        setOnFinished { opacity = opacityRange.endInclusive }
                        playFrom(initialDelayMillis.seconds)
                    }
                }
            }

            val uw = screenWidth / layout.numCols
            val uh = screenHeight / layout.numRows
            val bounds = BoundingBox((widget.pos.x - 1) * uw.toDouble(), (widget.pos.y - 1) * uh.toDouble(), widget.pos.width * uw.toDouble(), widget.pos.height * uh.toDouble())
            // draw a rectangular box in orange, with corner radius 10 px, inset by 10 pixels
            rectangle(bounds.minX + 10, bounds.minY + 10, bounds.width - 20, bounds.height - 20) {
                style =
                    "-fx-fill:none;-fx-stroke:#EE8F33;-fx-stroke-width:2;-fx-stroke-line-join:round;-fx-stroke-line-cap:round;-fx-stroke-dash-array:5 5;"
                arcWidth = 30.0
                arcHeight = 30.0
            }
            // draw a filled circle in upper left corner of inset rectangle of radius 10 in orange
            circle(bounds.minX + 30, bounds.maxY - 30, 15.0) {
                style = "-fx-fill:#EE8F33;"
            }
            // draw step # over the circle
            text(widget.overlay.step.toString()) {
                layoutX = bounds.minX + 22
                layoutY = bounds.maxY - 20
                style = "-fx-fill:black;-fx-font-size:28px;-fx-font-weight:bold;"
            }
            // fill in explanation in textflow box inside the rectangle
            textflow {
                resizeRelocate(bounds.minX + 50, bounds.maxY - 42, bounds.width - 60, bounds.height - 20)
                text(widget.overlay.explain) {
                    style = "-fx-fill:#EE8F33;-fx-font-size:17px;"
                }
            }
        }
    }

    //endregion

    //region PIPELINE MANAGEMENT

    private var job: Task<Unit>? = null
    private var jobCanceled = false

    init {
        runLater(3.seconds) { runPipeline() }
    }

    private fun runPipeline() {
        if (jobCanceled)
            return
        results.clearResults()
        job = runAsync {
            // TODO - add delay if isExplainerVisible is in place to slow down processing between steps for demo
//            StarshipPipelines.exec(config, results, if (isExplainerVisible) 3000 else 0)
            info<StarshipView>("Running Starship pipeline with delay=$isExplainerVisible...")
            runBlocking {
                StarshipPipelineExecutor(configs.pipeline, controller.chatService.value, results)
                    .execute()
            }
        }.also {
            it.setOnSucceeded {
                info<StarshipView>("Starship pipeline succeeded. Triggering a new run in 20 seconds.")
                runLater(20.seconds) { runPipeline() }
            }
        }
    }

    internal fun cancelPipeline() {
        info<StarshipView>("Canceling Starship pipeline...")
        job?.cancel()
        jobCanceled = true
        results.clearResults()
        info<StarshipView>("Cancellation succeeded.")
    }

    //endregion

    //region VIEW WIDGETS

    /** View element for large display text. */
    inner class AnimatingTextWidget(widget: StarshipConfigWidget, chromePane: Pane, value: ObservableValue<String>,
                                    buttonText: ObservableValue<String>? = null, buttonAction: (() -> Unit)? = null) {
        val view = AnimatingTextFlow()

        init {
            view.root.isMouseTransparent = true
            view.root.resizeRelocate(widget.px(), widget.py(), widget.pw(), widget.ph())
            view.updatePrefWidth(widget.pw())
            view.updateFontSize(widget.overlay.iconSize?.toDouble() ?: 12.0)
            chromePane.chromify(view.root, 36.0, widget.overlay.title ?: "", widget.overlay.icon, buttonText, buttonAction)
            value.onChange { view.animateText(FormattedText(it ?: "").toFxNodes()) }
        }
    }

    /** View element for displaying a list of thumbnails. */
    inner class ThumbnailWidget(list: ObservableList<DocumentThumbnail>) {
        private val DOC_THUMBNAIL_SIZE = 193

        val view = AnimatingThumbnailBox { }.apply {
            id = "starship-thumbnails"
            isMouseTransparent = true
            // TODO - dynamic location
            resizeRelocate(10.0, screenHeight - 300.0, screenWidth - 80.0, 300.0)
            spacing = 20.0
            list.onChange { animateThumbs(it.list) }
        }

        init {
            if (baseComponent is DocumentQaView) {
                val base = baseComponent as DocumentQaView
                base.snippets.onChange {
                    val thumbs = base.snippets.map { it.document.browsable()!! }.toSet()
                        .map { DocumentThumbnail(it, DocumentUtils.documentThumbnail(it, DOC_THUMBNAIL_SIZE, true)) }
                    view.animateThumbs(thumbs.take(6))
                }
            }
        }
    }

    //endregion

}

/** Utilities for decorating components. */
internal object Chromify {
    val WID2 = 4.0
    val STROKE_STYLE = "-fx-stroke: #333;"
    val CIRC_STYLE = "-fx-stroke: #333; -fx-fill:#777;"

    /** Add decoration to parent panel, for the given node which should be inside that parent pane. */
    internal fun Pane.chromify(
        node: Node, wid: Double, title: String, icon: FontAwesomeIcon? = null,
        buttonText: ObservableValue<String>? = null, buttonAction: (() -> Unit)? = null
    ) {
        val pane = pane {
            isPickOnBounds = false
            val gap = wid * 1.1
            val path = path {
                strokeWidth = wid
                strokeLineJoin = StrokeLineJoin.ROUND
                style = STROKE_STYLE
            }
            val circ = circle {
                strokeWidth = WID2
                style = CIRC_STYLE
                radius = 0.8 * wid
            }
            if (icon != null) {
                val iconView = icon.graphic.apply {
                    glyphSize = 0.9 * wid
                    glyphStyle = "-fx-fill: #333;"
                }
                val fudge = when (icon) {
                    FontAwesomeIcon.COMMENTS -> -2
                    FontAwesomeIcon.FILE_PDF_ALT -> -2
                    FontAwesomeIcon.MAGIC -> -1
                    else -> 0
                }
                iconView.layoutXProperty().bind(circ.centerXProperty() - iconView.layoutBounds.width/2 + fudge)
                iconView.layoutYProperty().bind(circ.centerYProperty() + iconView.layoutBounds.height/3.5)
                children.add(iconView)
            }
            if (buttonText != null) {
                line {
                    startXProperty().bind(circ.centerXProperty() + wid)
                    startYProperty().bind(circ.centerYProperty())
                    endXProperty().bind(circ.centerXProperty() + 2 * wid)
                    endYProperty().bind(circ.centerYProperty())
                    style {
                        stroke = c("#333")
                        strokeWidth = 4.px
                    }
                }
                button(buttonText) {
                    resizeRelocate(0.0, 0.0, 2 * wid, 2 * wid)
                    style {
                        backgroundColor += c("#333")
                        borderColor += box(c("#333"))
                        textFill = c("#777")
                        fontSize = (wid*0.8).px
                        borderRadius += box(10.px)
                        padding = box(0.px, 6.px, 1.px, 6.px)
                    }
                    action { buttonAction?.invoke() }
                    layoutXProperty().bind(circ.centerXProperty() + 2*wid)
                    layoutYProperty().bind(circ.centerYProperty() - 0.7*wid)

                    setOnMouseEntered {
                        style(true) { effect = javafx.scene.effect.DropShadow(10.0, c("#EE8F33")) }
                    }
                    setOnMouseExited {
                        // set to default effect
                        style(true) { effect = javafx.scene.effect.DropShadow(10.0, c("#333")) }
                    }
                }
                node.style += "-fx-padding: ${1.8*wid}px 0 0 0;"
            }
            text(title) {
                transforms.add(Rotate(90.0))
                style = "-fx-fill: gray; -fx-font-size: ${wid}px"
                layoutXProperty().bind(circ.centerXProperty().minus(0.3 * wid))
                layoutYProperty().bind(circ.centerYProperty().plus(wid))
            }

            fun updatePath() {
                // get bounds of node relative to [Pane]
                val boundsInScene = node.localToScene(node.boundsInLocal)
                val boundsInPane = this.sceneToLocal(boundsInScene)
                with(path) {
                    elements.clear()
                    moveTo(boundsInPane.minX - gap - wid / 2, boundsInPane.minY + 1.2 * wid)
                    lineTo(boundsInPane.minX - gap - wid / 2, boundsInPane.maxY)
                    arcTo(
                        gap + wid / 2, gap + wid / 2, 0.0,
                        boundsInPane.minX, boundsInPane.maxY + gap + wid / 2,
                        largeArcFlag = false, sweepFlag = false
                    )
                    lineTo(boundsInPane.maxX, boundsInPane.maxY + gap + wid / 2)
                }
                with(circ) {
                    centerX = boundsInPane.minX - gap - wid / 2
                    centerY = boundsInPane.minY + 0.5 * wid
                }
            }
            updatePath()
            node.boundsInParentProperty().onChange { updatePath() }
        }
        node.parentProperty().onChange { if (it == null) children.remove(pane) }
    }
}

