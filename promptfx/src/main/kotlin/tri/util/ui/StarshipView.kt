package tri.util.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
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
import tri.util.ui.Chromify.chromify

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

    private lateinit var thumbnails: AnimatingThumbnailBox

    val css = StarshipView::class.java.getResource("resources/chat.css")!!

    init {
        if (baseComponent is DocumentQaView) {
            val base = baseComponent as DocumentQaView
            base.snippets.onChange {
                val thumbs = base.snippets.map { it.document.browsable()!! }.toSet()
                    .map { DocumentThumbnail(it, DocumentUtils.documentThumbnail(it)) }
                thumbnails.animateThumbs(thumbs)
            }
        }
    }

    override val root = pane {
        stylesheets.add(css.toExternalForm())

        val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
            ?: Screen.getPrimary()
        val screenWidth = 1920 // curScreen.bounds.width
        val screenHeight = 1080 // curScreen.bounds.height

        // framing for a 3x3 grid
        if (true) {
            (0..2).forEach { x ->
                (0..2).forEach { y ->
                    rectangle(screenWidth * x / 3.0, screenHeight * y / 3.0, screenWidth / 3.0, screenHeight / 3.0) {
                        style = "-fx-stroke:black;-fx-fill:none"
                    }
                }
            }
        }

        add(input.apply {
            root.resizeRelocate(80.0, 60.0, 1140.0, 200.0)
            updatePrefWidth(1140.0)
            updateFontSize(36.0)
            results.input.onChange { animateText(FormattedText(it ?: "").toFxNodes()) }
            this@pane.chromify(root, wid = 24.0, title = "Question", icon = FontAwesomeIcon.QUESTION)
        })
        add(output.apply {
            root.resizeRelocate(120.0, 370.0, 1040.0, 800.0)
            updatePrefWidth(1040.0)
            updateFontSize(21.0)
            results.outputText.onChange { animateText(FormattedText(it ?: "").toFxNodes()) }
            this@pane.chromify(root, wid = 14.0, title = baseComponentTitle ?: "Answer", icon = FontAwesomeIcon.FILE_TEXT)
        })
        vbox(54.0) {
            resizeRelocate(1340.0, 400.0, 520.0, 400.0)
            bindChildren(results.secondaryOutputs) { output ->
                AnimatingTextFlow().apply {
                    updatePrefWidth(520.0)
                    updateFontSize(15.0)
                    animateText(output.text.toFxNodes())
                }.root.also {
                    this@pane.chromify(it, wid = 10.0, title = output.label)
                }
            }
        }

        thumbnails = AnimatingThumbnailBox { }.apply {
            id = "starship-thumbnails"
            resizeRelocate(30.0, screenHeight - 300.0, screenWidth - 80.0, 300.0)
            spacing = 20.0
        }
        results.thumbnails.onChange { thumbnails.animateThumbs(it.list) }
        add(thumbnails)

        results.started.onChange { if (it) indicator.startBlinking() }
        results.completed.onChange { indicator.stopBlinking() }

        add(indicator.apply {
            id = "starship-indicator"
            layoutX = 1920 - glyphSize.toDouble() - 20.0
            layoutY = 1080 - 20.0
        })

        // fill background with 100 twinkling stars of various sizes
        pane {
            for (i in 0 until 1000) {
                val star = FontAwesomeIcon.STAR
                val size = (5..20).random()
                add(BlinkingIndicator(star).apply {
                    layoutX = (size..(1920 - size).toInt()).random().toDouble()
                    layoutY = (size..(1080 - size).toInt()).random().toDouble() + size
                    opacity = (5..30).random().toDouble() / 100
                    glyphSize = size
                    glyphStyle = "-fx-fill:gray;"
                    initialDelayMillis = (500..1500).random()
                    blinkTimeMillis = 5000
                    opacityRange = (0.5 * opacity)..minOf(1.0, 1.5 * opacity)
                    startBlinking()
                })
            }
        }
    }

    fun runPipeline() {
        results.clearAll()
        runAsync {
            val config = StarshipPipelineConfig(controller.completionEngine.value)
            config.promptExec = object : AiPromptExecutor {
                override suspend fun exec(prompt: PromptWithParams, input: String): StarshipInterimResult {
                    var text: FormattedText? = null
                    (workspace as PromptFxWorkspace).sendInput(baseComponentTitle!!, input) { text = it }
                    return StarshipInterimResult(baseComponentTitle!!, text!!, null, emptyList())
                }
            }
            StarshipPipeline.exec(config, results)
        }
    }

    init {
        runLater(2.seconds) { runPipeline() }
        runLater(22.seconds) { runPipeline() }
        runLater(42.seconds) { runPipeline() }
        runLater(62.seconds) { runPipeline() }
    }

}

/** Utilities for decorating components. */
internal object Chromify {
    val WID2 = 4.0
    val STROKE_STYLE = "-fx-stroke: #333;"
    val CIRC_STYLE = "-fx-stroke: #333; -fx-fill:#777;"

    /** Add decoration to parent panel, for the given node which should be inside that parent pane. */
    internal fun Pane.chromify(node: Node, wid: Double, title: String, icon: FontAwesomeIcon? = null) {
        val pane = pane {
            val gap = wid * 1.5
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
                    glyphSize = 0.8 * wid
                    glyphStyle = "-fx-fill: #333;"
                }
                iconView.layoutXProperty().bind(circ.centerXProperty() - 0.4 * wid)
                iconView.layoutYProperty().bind(circ.centerYProperty() + 0.4 * wid)
                children.add(iconView)
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