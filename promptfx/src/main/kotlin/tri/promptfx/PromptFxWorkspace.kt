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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.stage.Screen
import javafx.stage.StageStyle
import tornadofx.*
import tri.promptfx.api.*
import tri.promptfx.tools.PromptTemplateView
import tri.util.ui.*

/** View configuration for the app. */
class PromptFxWorkspace : Workspace() {

    val views = mutableMapOf<String, Class<out UIComponent>>()
    var immersiveChatView: ImmersiveChatView? = null

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
        primaryStage.width = 1200.0
        primaryStage.height = 800.0
        with(leftDrawer) {
            group("OpenAI API", FontAwesomeIcon.CLOUD.graphic.navy) {
                (this as DrawerItem).padding = insets(5.0)
                hyperlinkview<ModelsView>("Models")
                separator { }
                label("Text APIs")
                hyperlinkview<ChatViewBasic>("Chat")
                hyperlinkview<ChatViewAdvanced>("Chat (Advanced)")
                hyperlinkview<CompletionsView>("Completions")
                separator { }
                label("Audio/Visual APIs")
                hyperlinkview<AudioView>("Audio")
                hyperlinkview<ImagesView>("Images")
                separator { }
                label("Advanced APIs")
                hyperlinkview<EmbeddingsView>("Embeddings")
                hyperlinkview<FineTuningApiView>("Fine-tuning")
                hyperlinkview<FilesView>("Files")
                hyperlinkview<ModerationsView>("Moderations")
                separator { }
                label("Documentation/Links")
                browsehyperlink("API Reference", "https://platform.openai.com/docs/api-reference")
                browsehyperlink("API Playground", "https://platform.openai.com/playground")
                browsehyperlink("API Pricing", "https://openai.com/pricing")
                browsehyperlink("OpenAI Blog", "https://openai.com/blog")
            }
            group("Tools", FontAwesomeIcon.WRENCH.graphic.navy) {
                // configured via [NavigableWorkspaceView] plugins
            }
            group("Documents", FontAwesomeIcon.FILE.graphic.navy) {
                // configured via [NavigableWorkspaceView] plugins
            }
            group("Text", FontAwesomeIcon.FONT.graphic.navy) {
                // configured via [NavigableWorkspaceView] plugins
            }
            group("Fun", FontAwesomeIcon.SMILE_ALT.graphic.navy) {
                // configured via [NavigableWorkspaceView] plugins
            }
            group("Audio", FontAwesomeIcon.MICROPHONE.graphic.navy) {
                // configured via [NavigableWorkspaceView] plugins

                // IDEAS for additional audio apps
                // - speech recognition
                // - speech translation
                // - speech synthesis
            }
            group("Vision", FontAwesomeIcon.IMAGE.graphic.navy) {
                // configured via [NavigableWorkspaceView] plugins

                // IDEAS for additional image apps
                // - automatic image captioning
                // - visual question answering
                // - style/pose/depth transfer, inpainting, outpainting, etc.
                // - optical character recognition
                // - image classification, object detection, facial recognition, etc.
                // - image segmentation, depth, pose estimation, gaze estimation, etc.
                // - image enhancement, super-resolution, denoising, inpainting, deblurring, etc.
            }
            group("Integrations", FontAwesomeIcon.PLUG.graphic.navy) {
                // configured via [NavigableWorkspaceView] plugins
            }
            group("Documentation", FontAwesomeIcon.BOOK.graphic.navy) {
                // nothing here, but testing to see this doesn't show up in view

                // configured via [NavigableWorkspaceView] plugins
            }
        }
    }

    //region HOOKS FOR SPECIFIC VIEWS

    /** Looks up a view by name. */
    fun findTaskView(name: String): AiTaskView? {
        return views[name]?.let { find(it) } as? AiTaskView
    }

    /** Launches the template view with the given prompt text. */
    fun launchTemplateView(prompt: String) {
        val view = find<PromptTemplateView>()
        view.template.set(prompt)
        workspace.dock(view)
    }

    //endregion

    private fun enterFullScreenMode() {
        val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
            ?: Screen.getPrimary()
        find<ImmersiveChatView>(params = mapOf(
            "baseComponentTitle" to dockedComponent?.title,
            "baseComponent" to dockedComponent
        )).apply {
            immersiveChatView = this
        }.openWindow(
            StageStyle.UNDECORATED
        )!!.apply {
            x = curScreen.bounds.minX
            y = curScreen.bounds.minY
            width = curScreen.bounds.width
            height = curScreen.bounds.height
            isMaximized = true
            scene.root.style = "-fx-base:black"
            onHidden = EventHandler { immersiveChatView = null }
        }
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
        views[name] = T::class.java
        hyperlink(name) {
            action {
                isVisited = false
                dock<T>()
            }
        }
    }

    private fun EventTarget.hyperlinkview(view: NavigableWorkspaceView) {
        if (view is NavigableWorkspaceViewImpl<*>) {
            views[view.name] = view.type.java
        }
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
