package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.TextArea
import javafx.stage.StageStyle
import tornadofx.*
import tri.promptfx.api.*
import tri.promptfx.`fun`.ChatBackView
import tri.promptfx.`fun`.ColorView
import tri.promptfx.`fun`.EmojiView
import tri.util.ui.ImmersiveChatView
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.graphic
import tri.util.ui.gray

/** View configuration for the app. */
class PromptFxWorkspace : Workspace() {

    private val controller: PromptFxController by inject()

    init {
        add(find<AiEngineView>())
        button(graphic = FontAwesomeIcon.SLIDESHARE.graphic).action {
            enterFullScreenMode()
        }
        root.bottom {
            add(find<AiProgressView>())
        }
    }

    init {
        primaryStage.width = 800.0
        primaryStage.height = 600.0
        with(leftDrawer) {
            group("OpenAI API", FontAwesomeIcon.CLOUD.graphic.gray) {
                hyperlinkview<ModelsView>("Models")
                hyperlinkview<CompletionsView>("Completions")
                hyperlinkview<ChatView>("Chat")
                hyperlinkview<EditsView>("Edits")
                hyperlinkview<ImagesView>("Images")
                hyperlinkview<EmbeddingsView>("Embeddings")
                hyperlinkview<AudioView>("Audio")
                hyperlinkview<FilesView>("Files")
                hyperlinkview<FineTuneView>("Fine-tunes")
                hyperlinkview<ModerationsView>("Moderations")
                separator { }
                browsehyperlink("API Reference", "https://platform.openai.com/docs/api-reference")
                browsehyperlink("API Playground", "https://platform.openai.com/playground")
                browsehyperlink("API Pricing", "https://openai.com/pricing")
                browsehyperlink("OpenAI Blog", "https://openai.com/blog")
            }
            group("Text", FontAwesomeIcon.FILE.graphic.gray) {
                // content added as plugins
            }
            group("Fun", FontAwesomeIcon.SMILE_ALT.graphic.gray) {
                hyperlinkview<ColorView>("Text to Color")
                hyperlinkview<EmojiView>("Text to Emoji")
                hyperlinkview<ChatBackView>("AI-AI Chat")
            }
            group("Audio", FontAwesomeIcon.MICROPHONE.graphic.gray) {
                hyperlinkview<AudioView>("Audio")
                // IDEAS for additional audio apps
                // - speech recognition
                // - speech translation
                // - speech synthesis
            }
            group("Vision", FontAwesomeIcon.IMAGE.graphic.gray) {
                hyperlinkview<ImagesView>("Images")
                // IDEAS for additional image apps
                // - automatic image captioning
                // - visual question answering
                // - style/pose/depth transfer, inpainting, outpainting, etc.
                // - optical character recognition
                // - image classification, object detection, facial recognition, etc.
                // - image segmentation, depth, pose estimation, gaze estimation, etc.
                // - image enhancement, super-resolution, denoising, inpainting, deblurring, etc.
            }
            group("Integrations", FontAwesomeIcon.PLUG.graphic.gray) {
                // content added as plugins
            }
            group("Documentation", FontAwesomeIcon.BOOK.graphic.gray) {
                // nothing here, but testing to see this doesn't show up in view
            }
        }
    }

    init {
        dock<ModelsView>()
    }

    private fun enterFullScreenMode() {
        find<ImmersiveChatView>(params = mapOf(
            "onUserRequest" to userInputFunction(),
            "baseComponentTitle" to dockedComponent?.title,
            "baseComponent" to dockedComponent
        )).openWindow(
            StageStyle.UNDECORATED
        )!!.apply {
            x = 0.0
            y = 0.0
            isMaximized = true
            scene.root.style = "-fx-base:black"
        }
    }

    private fun userInputFunction(): suspend (String) -> String = { input ->
        (dockedComponent as? AiTaskView)?.let { view ->
            view.inputPane.children.filterIsInstance<TextArea>().firstOrNull()?.let {
                it.text = input
                val result = view.processUserInput()
                result.finalResult.toString()
            }
        } ?: "This view doesn't support processing user input.\nInput was: $input"
    }

    //region LAYOUT

    private fun Drawer.group(title: String, icon: Node? = null, op: EventTarget.() -> Unit) {
        item(title, icon, expanded = false) {
            op()
            NavigableWorkspaceView.viewPlugins.filter { it.category == title }.forEach {
                hyperlinkview(it)
            }
        }.apply {
            if (children.isEmpty()) {
                removeFromParent()
            }
        }
    }

    private inline fun <reified T: UIComponent> EventTarget.hyperlinkview(name: String) {
        hyperlink(name) {
            action {
                isVisited = false
                dock<T>()
            }
        }
    }

    private fun EventTarget.hyperlinkview(view: NavigableWorkspaceView) {
        hyperlink(view.name) {
            action {
                isVisited = false
                view.dock(this@PromptFxWorkspace)
            }
        }
    }

    private fun EventTarget.browsehyperlink(label: String, url: String) {
        hyperlink(label) {
            action { hostServices.showDocument(url) }
        }
    }

    //endregion

}