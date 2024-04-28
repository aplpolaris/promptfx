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
package tri.util.ui.starship

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.animation.Timeline
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableValue
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.geometry.BoundingBox
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.scene.shape.StrokeLineJoin
import javafx.scene.transform.Rotate
import javafx.stage.Screen
import tornadofx.*
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxDriver.sendInput
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.docs.FormattedText
import tri.promptfx.docs.toFxNodes
import tri.util.info
import tri.util.ui.*
import tri.util.ui.AnimatingThumbnailBox
import tri.util.ui.starship.Chromify.chromify
import tri.util.ui.starship.StarshipContentConfig.explain

/** View for a full-screen animated text display. */
class StarshipView : Fragment("Starship") {

    val baseComponentTitle: String? by param()
    val baseComponent: View? by param()

    val controller: PromptFxController by inject()
    val results = StarshipPipelineResults()

    val indicator = BlinkingIndicator(FontAwesomeIcon.ROCKET).apply {
        glyphSize = 60.0
        glyphStyle = "-fx-fill:gray;"
    }
    val input = AnimatingTextFlow()
    val output = AnimatingTextFlow()
    val outputHighlight = AnimatingTextFlow()

    //region BUTTON VARS

    private val summarizeForIndex = SimpleIntegerProperty(0)
    private val summarizeFor = summarizeForIndex.stringBinding { summarizeForOptions[it!!.toInt()] }
    private val summarizeForOptions = StarshipContentConfig.userOptions["text-simplify-audience"]!!["audience"]!!
    private fun nextSummarizeFor() { summarizeForIndex.set((summarizeForIndex.get() + 1) % summarizeForOptions.size) }

    private val targetLanguageIndex = SimpleIntegerProperty(0)
    private val targetLanguage = targetLanguageIndex.stringBinding { targetLanguageOptions[it!!.toInt()] }
    private val targetLanguageOptions = StarshipContentConfig.userOptions["translate-text"]!!["instruct"]!!
    private fun nextTargetLanguage() { targetLanguageIndex.set((targetLanguageIndex.get() + 1) % targetLanguageOptions.size) }

    //endregion

    private lateinit var thumbnails: AnimatingThumbnailBox
    private val DOC_THUMBNAIL_SIZE = 193
    private var isExplainerVisible = false

    private val css1 = ImmersiveChatView::class.java.getResource("resources/chat.css")!!
    private val css2 = StarshipView::class.java.getResource("resources/starship.css")!!

    init {
        if (baseComponent is DocumentQaView) {
            val base = baseComponent as DocumentQaView
            base.snippets.onChange {
                val thumbs = base.snippets.map { it.document.browsable()!! }.toSet()
                    .map { DocumentThumbnail(it, DocumentUtils.documentThumbnail(it, DOC_THUMBNAIL_SIZE, true)) }
                thumbnails.animateThumbs(thumbs.take(6))
            }
        }
    }

    override val root = pane {
        stylesheets.add(css1.toExternalForm())
        stylesheets.add(css2.toExternalForm())

        val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
            ?: Screen.getPrimary()
        val screenWidth = 1920 // curScreen.bounds.width
        val screenHeight = 1080 // curScreen.bounds.height

        // framing for a 3x3 grid
        if (StarshipContentConfig.isShowGrid) {
            (0..2).forEach { x ->
                (0..2).forEach { y ->
                    rectangle(screenWidth * x / 3.0, screenHeight * y / 3.0, screenWidth / 3.0, screenHeight / 3.0) {
                        style = "-fx-stroke:black;-fx-fill:none"
                    }
                }
            }
        }

        val chromePane = pane {
            resizeRelocate(0.0, 0.0, 5.0, 5.0)
        }

        add(input.apply {
            root.isMouseTransparent = true
            root.resizeRelocate(120.0, 60.0, 1080.0, 200.0)
            updatePrefWidth(1080.0)
            updateFontSize(48.0)
            results.input.onChange { animateText(FormattedText(it ?: "").toFxNodes()) }
            chromePane.chromify(root, wid = 36.0, title = "Question", icon = FontAwesomeIcon.QUESTION)
        })
        add(output.apply {
            root.isMouseTransparent = true
            root.resizeRelocate(55.0, 380.0, 525.0, 800.0)
            updatePrefWidth(525.0)
            updateFontSize(20.0)
            results.outputText.onChange { animateText(FormattedText(it ?: "").toFxNodes()) }
            chromePane.chromify(root, wid = 15.0, title = baseComponentTitle ?: "Answer", icon = FontAwesomeIcon.FILE_PDF_ALT)
        })
        add(outputHighlight.apply {
            root.isMouseTransparent = true
            root.visibleWhen(results.outputHighlightText.isNotBlank())
            root.resizeRelocate(700.0, 380.0, 520.0, 800.0)
            updatePrefWidth(520.0)
            updateFontSize(21.0)
            results.outputHighlightText.onChange { animateText(FormattedText(it ?: "").toFxNodes()) }
            chromePane.chromify(root, wid = 17.5, title = "Summarize For", icon = FontAwesomeIcon.COMMENTS,
                buttonText = summarizeFor, buttonAction = ::nextSummarizeFor
            )
        })
        vbox(54.0) {
            resizeRelocate(1340.0, 60.0, 520.0, 400.0)
            bindChildren(results.secondaryOutputs) { output ->
                AnimatingTextFlow().apply {
                    updatePrefWidth(520.0)
                    updateFontSize(15.0)
                    animateText(output.text.toFxNodes())
                }.root.also {
                    isMouseTransparent = true
                    val bt = if (output.label == "Translate") targetLanguage else null
                    val ba = if (output.label == "Translate") ::nextTargetLanguage else null
                    chromePane.chromify(it, wid = 12.0, title = output.label, icon = FontAwesomeIcon.MAGIC, buttonText = bt, buttonAction = ba)
                }
            }
        }

        thumbnails = AnimatingThumbnailBox { }.apply {
            id = "starship-thumbnails"
            isMouseTransparent = true
            resizeRelocate(10.0, screenHeight - 300.0, screenWidth - 80.0, 300.0)
            spacing = 20.0
        }
        results.thumbnails.onChange { thumbnails.animateThumbs(it.list) }
        add(thumbnails)

        results.started.onChange { if (it) indicator.startBlinking() }
        results.completed.onChange { indicator.stopBlinking() }

        add(indicator.apply {
            id = "starship-indicator"
            layoutX = screenWidth - glyphSize.toDouble() - 20.0
            layoutY = screenHeight - 20.0
        })

        // fill background with 100 twinkling stars of various sizes
        pane {
            isMouseTransparent = true
            for (i in 0 until StarshipContentConfig.backgroundIconCount) {
                val star = StarshipContentConfig.backgroundIcon
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

        // explainer overlay in orange
        val explainer = pane {
            isMouseTransparent = true
            isVisible = false
            explainerOverlay(0, 0, xn = 2, yn = 1, screenWidth, screenHeight, step = 1, explain = explain[0])
            explainerOverlay(0, 2, xn = 2, yn = 1, screenWidth, screenHeight, step = 2, explain = explain[1])
            explainerOverlay(0, 1, xn = 1, yn = 1, screenWidth, screenHeight, step = 3, explain = explain[2])
            explainerOverlay(1, 1, xn = 1, yn = 1, screenWidth, screenHeight, step = 4, explain = explain[3])
            explainerOverlay(2, 0, xn = 1, yn = 3, screenWidth, screenHeight, step = 5, explain = explain[4])
        }
        onKeyPressed = EventHandler { event ->
            if (event.code == KeyCode.X) {
                explainer.isVisible = !explainer.isVisible
                isExplainerVisible = explainer.isVisible
            }
        }
    }

    private fun Pane.explainerOverlay(x: Int, y: Int, xn: Int, yn: Int, w: Int, h: Int, step: Int, explain: String) {
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
                } else if (it == step) {
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

            val uw = w / 3
            val uh = h / 3
            val bounds = BoundingBox(x * uw.toDouble(), y * uh.toDouble(), xn * uw.toDouble(), yn * uh.toDouble())
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
            text(step.toString()) {
                layoutX = bounds.minX + 22
                layoutY = bounds.maxY - 20
                style = "-fx-fill:black;-fx-font-size:28px;-fx-font-weight:bold;"
            }
            // fill in explanation in textflow box inside the rectangle
            textflow {
                resizeRelocate(bounds.minX + 50, bounds.maxY - 42, bounds.width - 60, bounds.height - 20)
                text(explain) {
                    style = "-fx-fill:#EE8F33;-fx-font-size:17px;"
                }
            }
        }
    }

    private var job: Task<Unit>? = null
    private var jobCanceled = false

    init {
        runLater(3.seconds) { runPipeline() }
    }

    private fun runPipeline() {
        if (jobCanceled)
            return
        results.clearAll()
        job = runAsync {
            val config = StarshipPipelineConfig(controller.completionEngine.value)
            config.secondaryPrompts[0].params["audience"] = summarizeFor.value
            config.secondaryPrompts[3].params["instruct"] = targetLanguage.value
            config.promptExec = object : AiPromptExecutor {
                override suspend fun exec(prompt: PromptWithParams, input: String): StarshipInterimResult {
                    var text: FormattedText? = null
                    (workspace as PromptFxWorkspace).sendInput(baseComponentTitle!!, input) { text = it }
                    return StarshipInterimResult(baseComponentTitle!!, text!!, null, emptyList())
                }
            }
            info<StarshipView>("Running Starship pipeline with delay=$isExplainerVisible...")
            StarshipPipeline.exec(config, results, if (isExplainerVisible) 3000 else 0)
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
        results.clearAll()
        info<StarshipView>("Cancellation succeeded.")
    }

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

