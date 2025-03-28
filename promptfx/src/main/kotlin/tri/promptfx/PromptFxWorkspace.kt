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
package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.stage.Screen
import javafx.stage.StageStyle
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.api.*
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.library.TextLibraryInfo
import tri.promptfx.tools.PromptTemplateView
import tri.promptfx.library.TextManagerView
import tri.promptfx.tools.PromptTraceHistoryView
import tri.promptfx.ui.NavigableWorkspaceViewRuntime
import tri.util.ui.*
import tri.util.ui.starship.StarshipView

/** View configuration for the app. */
class PromptFxWorkspace : Workspace() {

    val promptFxConfig : PromptFxConfig by inject()

    val views = mutableMapOf<String, MutableMap<String, PromptFxViewInfo>>()
    var immersiveChatView: ImmersiveChatView? = null

    //region VIEW LOOKUPS

    val viewsWithInputs
        get() = views.filter { it.affordances.acceptsInput }
    val viewsWithCollections
        get() = views.filter { it.affordances.acceptsCollection }
    val viewsWithOutputs
        get() = views.filter { it.affordances.producesOutput }

    private fun <X> Map<String, Map<String, X>>.filter(predicate: (X) -> Boolean) =
        mapValues { it.value.filterValues(predicate) }.filterValues { it.isNotEmpty() }

    //endregion

    init {
        add(find<AiEngineView>())
        with(PromptFxModels.policy) {
            if (isShowBanner) {
                add(Label(bar.text).apply {
                    padding = insets(0.0, 20.0, 0.0, 20.0)
                    style {
                        fontWeight = javafx.scene.text.FontWeight.BOLD
                        fontSize = 18.px
                        backgroundColor += bar.bgColor
                        textFill = bar.fgColor
                    }
                })
            }
        }
        button(graphic = FontAwesomeIcon.SLIDESHARE.graphic).action {
            enterFullScreenMode()
        }
        if (promptFxConfig.isStarshipEnabled) {
            button(graphic = FontAwesomeIcon.ROCKET.graphic).action {
                enterStarshipMode()
            }
        }
        root.bottom {
            add(find<AiProgressView>())
        }
    }

    init {
        primaryStage.width = 1200.0
        primaryStage.height = 800.0
        with(leftDrawer) {
            group("API", FontAwesomeIcon.CLOUD.graphic.forestGreen) {
                (this as DrawerItem).padding = insets(5.0)
                hyperlinkview<ModelsView>("API", "Models")
                separator { }
                label("Text APIs")
                hyperlinkview<ChatViewBasic>("API", "Chat")
                hyperlinkview<ChatViewAdvanced>("API", "Chat (Advanced)")
                hyperlinkview<CompletionsView>("API", "Completions")
                separator { }
                label("Audio/Visual APIs")
                hyperlinkview<AudioView>("API", "Audio")
                hyperlinkview<AudioSpeechView>("API", "Speech")
                hyperlinkview<ImagesView>("API", "Images")
                separator { }
                label("Advanced APIs")
                hyperlinkview<EmbeddingsView>("API", "Embeddings")
//                hyperlinkview<FineTuningApiView>("API", "Fine-tuning")
//                hyperlinkview<FilesView>("API", "Files")
                hyperlinkview<ModerationsView>("API", "Moderations")
                separator { }
                label("Documentation/Links")
                browsehyperlink("PromptFx Wiki", "https://github.com/aplpolaris/promptfx/wiki")
                separator { }
                browsehyperlink("OpenAI API Reference", "https://platform.openai.com/docs/api-reference")
                browsehyperlink("OpenAI API Playground", "https://platform.openai.com/playground")
                browsehyperlink("OpenAI API Pricing", "https://openai.com/pricing")
                browsehyperlink("OpenAI Blog", "https://openai.com/blog")
                separator { }
                browsehyperlink("Gemini API Reference", "https://ai.google.dev/api/generate-content")
                separator { }
                browsehyperlink("Mustache Template Docs", "https://mustache.github.io/mustache.5.html")
            }
            (RuntimePromptViewConfigs.categories() - setOf("API", "Tools", "Documents", "Text", "Fun", "Audio", "Vision", "Integrations", "Documentation")).forEach {
                group(it, FontAwesomeIcon.COG.graphic.forestGreen) { }
            }
            // views below are configured via [NavigableWorkspaceView] plugins
            group("Tools", FontAwesomeIcon.WRENCH.graphic.forestGreen)
            group("Documents", FontAwesomeIcon.FILE.graphic.forestGreen)
            group("Text", FontAwesomeIcon.FONT.graphic.forestGreen)
            group("Fun", FontAwesomeIcon.SMILE_ALT.graphic.forestGreen)
            group("Audio", FontAwesomeIcon.MICROPHONE.graphic.forestGreen)
            group("Vision", FontAwesomeIcon.IMAGE.graphic.forestGreen)
            group("Integrations", FontAwesomeIcon.PLUG.graphic.forestGreen)

            group("Documentation", FontAwesomeIcon.BOOK.graphic.forestGreen) {
                // nothing here, but testing to see this doesn't show up in view
            }
        }
    }

    override fun onBeforeShow() {
        dock<DocumentQaView>()
    }

    //region HOOKS FOR SPECIFIC VIEWS

    /** Looks up a view by name. */
    fun findTaskView(name: String): AiTaskView? {
        return views.values.map { it.entries }.flatten()
            .find { it.key == name }?.let { it.value.viewComponent ?: find(it.value.view!!) } as? AiTaskView
    }

    /** Launches the template view with the given prompt trace. */
    fun launchHistoryView(prompt: AiPromptTraceSupport<*>) {
        val view = find<PromptTraceHistoryView>()
        view.selectPromptTrace(prompt)
        workspace.dock(view)
    }

    /** Launches the template view with the given prompt trace. */
    fun launchTemplateView(prompt: AiPromptTraceSupport<*>) {
        val view = find<PromptTemplateView>()
        view.importPromptTrace(prompt)
        workspace.dock(view)
    }

    /** Launches the template view with the given prompt text. */
    fun launchTemplateView(prompt: String) {
        val view = find<PromptTemplateView>()
        view.template.set(prompt)
        workspace.dock(view)
    }

    /** Launches the text manager view with the given library. */
    fun launchTextManagerView(library: TextLibrary) {
        val view = find<TextManagerView>()
        view.loadTextLibrary(TextLibraryInfo(library, null))
        workspace.dock(view)
    }

    //endregion

    //region FULL-SCREEN WINDOW

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

    private fun enterStarshipMode() {
        val curScreen = Screen.getScreensForRectangle(primaryStage.x, primaryStage.y, 1.0, 1.0).firstOrNull()
            ?: Screen.getPrimary()
        val view = find<StarshipView>(params = mapOf(
            "baseComponentTitle" to dockedComponent?.title,
            "baseComponent" to dockedComponent
        ))
        view.openWindow(
            StageStyle.UNDECORATED
        )!!.apply {
            x = curScreen.bounds.minX
            y = curScreen.bounds.minY
            width = curScreen.bounds.width
            height = curScreen.bounds.height
            isMaximized = true
            scene.root.style = "-fx-base:black"
            setOnHiding { view.cancelPipeline() }
        }
    }

    //endregion

    //region LAYOUT

    private fun Drawer.group(category: String, icon: Node? = null, op: EventTarget.() -> Unit = { }) {
        item(category, icon, expanded = false) {
            op()
            val viewsById = NavigableWorkspaceView.viewPlugins.filter { it.category == category }
                .associateBy { it.name }
                .toSortedMap()
            val viewsDefinedAtRuntime = RuntimePromptViewConfigs.configs(category)
                .filter { it.title !in viewsById }
                .associate { it.title to NavigableWorkspaceViewRuntime(it) }
            viewsById.putAll(viewsDefinedAtRuntime)
            viewsById.values.forEach {
                hyperlinkview(category, it)
            }
        }.apply {
            if (children.isEmpty()) {
                removeFromParent()
            }
        }
    }

    private inline fun <reified T: UIComponent> EventTarget.hyperlinkview(viewGroup: String, name: String) {
        if (PromptFxModels.policy.supportsView(T::class.java.simpleName)) {
            views.getOrPut(viewGroup) { mutableMapOf() }.getOrPut(name) {
                PromptFxViewInfo(viewGroup, name, T::class.java)
            }
            hyperlink(name) {
                action {
                    isVisited = false
                    dock<T>()
                }
            }
        }
    }

//    private fun EventTarget.hyperlinkview(viewGroup: String, view: NavigableWorkspaceView) {
//        if (PromptFxModels.policy.supportsView((view as? NavigableWorkspaceViewImpl<*>)?.type?.simpleName ?: "")) {
//            if (view is NavigableWorkspaceViewImpl<*>) {
//                views.getOrPut(viewGroup) { mutableMapOf() }.getOrPut(view.name) {
//                    PromptFxViewInfo(viewGroup, view.name, view.type.java, affordances = view.affordances)
//                }
//            }
//            hyperlink(view.name) {
//                action {
//                    isVisited = false
//                    view.dock(this@PromptFxWorkspace)
//                }
//            }
//        }
//    }

    private fun EventTarget.hyperlinkview(viewGroup: String, view: NavigableWorkspaceView) {
        val viewSimpleName = (view as? NavigableWorkspaceViewImpl<*>)?.type?.simpleName ?: view.name
        if (PromptFxModels.policy.supportsView(viewSimpleName)) {
            if (view is NavigableWorkspaceViewImpl<*>) {
                views.getOrPut(viewGroup) { mutableMapOf() }.getOrPut(view.name) {
                    PromptFxViewInfo(viewGroup, view.name, view.type.java, affordances = view.affordances)
                }
            } else if (view is NavigableWorkspaceViewRuntime) {
                views.getOrPut(viewGroup) { mutableMapOf() }.getOrPut(view.name) {
                    PromptFxViewInfo(viewGroup, view.name, viewComponent = view.view, affordances = view.affordances)
                }
            }
            hyperlink(view.name) {
                action {
                    isVisited = false
                    view.dock(this@PromptFxWorkspace)
                }
            }
        }
    }

    private fun EventTarget.browsehyperlink(label: String, url: String) {
        hyperlink(label, graphic = FontAwesomeIcon.EXTERNAL_LINK.graphic) {
            action { hostServices.showDocument(url) }
        }
    }

    //endregion

}
