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
package tri.promptfx.ui

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.stage.Screen
import kotlinx.coroutines.runBlocking
import tornadofx.*
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxDriver.sendInput
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxWorkspace
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.docs.DocumentQaView.Companion.browseToBestSnippet
import tri.ai.text.docs.FormattedText
import tri.util.ui.AnimatingTextFlow
import tri.util.ui.AnimatingThumbnailBox
import tri.util.ui.BlinkingIndicator
import tri.util.ui.DocumentThumbnail
import tri.util.ui.DocumentUtils.documentThumbnail

/** View for a full-screen chat display. */
class ImmersiveChatView : Fragment("Immersive Chat") {

    val baseComponentTitle: String? by param()
    val baseComponent: View? by param()
    val inputFontSize = SimpleIntegerProperty(48)

    val indicator = BlinkingIndicator(FontAwesomeIcon.ROCKET).also {
        it.glyphSize = 60.0
        it.glyphStyle = "-fx-fill: white;"
    }

    val controller: PromptFxController by inject()
    val input = SimpleStringProperty("")

    private lateinit var inputField: TextField
    private lateinit var output: AnimatingTextFlow
    private lateinit var thumbnails: AnimatingThumbnailBox

    val css = ImmersiveChatView::class.java.getResource("resources/chat.css")!!

    init {
        if (baseComponent is DocumentQaView) {
            val base = baseComponent as DocumentQaView
            base.snippets.onChange {
                val thumbs = base.snippets.map { it.document.browsable()!! }.toSet()
                    .take(MAX_THUMBNAILS_FOR_IMMERSIVE_CHAT_VIEW)
                    .map { DocumentThumbnail(it, documentThumbnail(it, DOC_THUMBNAIL_SIZE, false)) }
                thumbnails.animateThumbs(thumbs)
            }
        }
    }

    override val root = vbox {
        alignment = Pos.CENTER
        spacing = 20.0
        stylesheets.add(css.toExternalForm())

        val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
            ?: Screen.getPrimary()
        val screenHeight = curScreen.bounds.height

        // add spacer with height 0.1
        vbox {
            prefHeight = 0.1 * screenHeight
            padding = insets(10.0, 0.0, 0.0, 0.0)
            hbox(50.0) {
                alignment = Pos.CENTER
                addPolicyBox()
                indicator.attachTo(this)
                addPolicyBox()
            }
        }

        text("You are in: ${baseComponentTitle ?: "Test"} Mode") {
            alignment = Pos.CENTER
            prefHeight = 0.07 * screenHeight
            style = "-fx-font-size: 24px; -fx-fill: gray; -fx-font-weight: bold;"
        }

        inputField = textfield(input) {
            id = "chat-input"
            prefHeight = 0.15 * screenHeight
            alignment = Pos.CENTER
            action { handleUserAction { } }
            inputFontSize.onChange {
                style = "-fx-font-size: ${it}px;"
            }
            // when pressing up or down adjust the font size
            setOnKeyPressed {
                when (it.code) {
                    javafx.scene.input.KeyCode.UP -> inputFontSize.value += 2
                    javafx.scene.input.KeyCode.DOWN -> inputFontSize.value -= 2
                    else -> {}
                }
            }
        }

        output = AnimatingTextFlow().apply {
            root.prefHeight = 0.3 * screenHeight
            root.alignment = Pos.CENTER
        }
        add(output)

        val action: ((DocumentThumbnail) -> Unit)? = when (val view = baseComponent) {
            is DocumentQaView ->
                { doc -> browseToBestSnippet(doc.document, view.planner.lastResult, hostServices) }
            else -> null
        }
        thumbnails = AnimatingThumbnailBox(action).apply {
            alignment = Pos.CENTER
            prefHeight = 0.22 * screenHeight
            spacing = 40.0
        }
        add(thumbnails)

        vbox {
            prefHeight = 0.02 * screenHeight
            padding = insets(10.0, 0.0, 0.0, 0.0)
        }
    }

    //region INPUT/OUTPUT

    internal fun setUserInput(text: String, callback: (FormattedText) -> Unit) {
        input.set(text)
        handleUserAction(callback)
    }

    private fun handleUserAction(callback: (FormattedText) -> Unit) {
        runLater {
            output.textNodes.setAll()
            indicator.startBlinking()
        }
        runAsync {
            runBlocking {
                find<PromptFxWorkspace>().sendInput(baseComponentTitle!!, input.value, callback)
            }
        } ui {
            indicator.stopBlinking()
            controller.updateUsage()
            if (baseComponent is DocumentQaView) {
                (baseComponent as DocumentQaView).enableHyperlinkActions(listOf(it))
            }
            // Calculate and set automated font size based on response length
            val fontSize = calculateOptimalFontSize(it.toString())
            output.updateFontSize(fontSize)
            output.animateText(it.toFxNodes(), onFinished = {
                (root.scene.lookup("#chat-input") as TextField).selectAll()
            })
        }
    }

    //endregion

    //region FONT SIZE CALCULATION

    /**
     * Calculates optimal font size for response text based on available text area and content length.
     * Uses area-based approach to achieve optimal text density for readability.
     */
    private fun calculateOptimalFontSize(text: String): Double {
        val textLength = text.length
        
        // Define font size bounds
        val minFontSize = 12.0
        val maxFontSize = 96.0
        
        // Get the TextFlow dimensions from the output component
        val textFlow = try {
            (((output.root.children[0] as javafx.scene.control.ScrollPane).content as javafx.scene.layout.HBox).children[0] as javafx.scene.text.TextFlow)
        } catch (e: Exception) {
            // Fallback to simple length-based calculation if we can't access dimensions
            return calculateFontSizeByLength(textLength, minFontSize, maxFontSize)
        }
        
        // Get available area (use layout bounds for actual rendered size)
        val availableWidth = if (textFlow.width > 0) textFlow.width else textFlow.prefWidth
        val availableHeight = if (textFlow.height > 0) textFlow.height else 300.0 // reasonable fallback
        val availableArea = availableWidth * availableHeight
        
        if (availableArea <= 0 || textLength == 0) {
            return calculateFontSizeByLength(textLength, minFontSize, maxFontSize)
        }
        
        // Calculate font size to achieve target area usage
        // Each character takes approximately font_size² * 0.72 pixels (width ratio ~0.6, height ratio ~1.2)
        // Target: character_area * text_length * 1.5 = available_area
        val characterAreaFactor = 0.72 * 1.5 // Character area multiplier * target density factor
        val idealFontSizeSquared = availableArea / (textLength * characterAreaFactor)
        val idealFontSize = kotlin.math.sqrt(idealFontSizeSquared)
        
        // Clamp to reasonable bounds
        return idealFontSize.coerceIn(minFontSize, maxFontSize)
    }
    
    /**
     * Fallback font size calculation based purely on text length.
     * Used when area-based calculation is not possible.
     */
    private fun calculateFontSizeByLength(textLength: Int, minFontSize: Double, maxFontSize: Double): Double {
        // Define text length thresholds
        val shortTextThreshold = 100    // Very short responses get max font size
        val longTextThreshold = 2000    // Very long responses get min font size
        
        return when {
            textLength <= shortTextThreshold -> maxFontSize
            textLength >= longTextThreshold -> minFontSize
            else -> {
                // Linear interpolation between max and min font size
                val ratio = (textLength - shortTextThreshold).toDouble() / (longTextThreshold - shortTextThreshold)
                maxFontSize - (ratio * (maxFontSize - minFontSize))
            }
        }
    }

    //endregion

    companion object {
        private const val DOC_THUMBNAIL_SIZE = 240
        private const val MAX_THUMBNAILS_FOR_IMMERSIVE_CHAT_VIEW = 8
    }

}

/** Add a box associated with the global model access policy. */
internal fun EventTarget.addPolicyBox() {
    if (!PromptFxModels.policy.isShowBanner) return
    label(PromptFxModels.policy.bar.text) {
        padding = insets(0.0, 5.0, 0.0, 5.0)
        alignment = Pos.CENTER
        style {
            fontSize = 24.px
            fontWeight = javafx.scene.text.FontWeight.BOLD
            fill = PromptFxModels.policy.bar.fgColorDark
            backgroundColor += PromptFxModels.policy.bar.bgColorDark
            borderRadius += box(10.px)
            backgroundRadius += box(10.px)
        }
    }
}
