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
package tri.promptfx

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.scene.control.Label
import javafx.stage.Screen
import javafx.stage.StageStyle
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.ai.text.chunks.TextLibrary
import tri.promptfx.api.*
import tri.promptfx.docs.DocumentQaView
import tri.promptfx.docs.TextLibraryInfo
import tri.promptfx.docs.TextManagerView
import tri.promptfx.mcp.*
import tri.promptfx.multimodal.AudioSpeechView
import tri.promptfx.multimodal.AudioView
import tri.promptfx.prompts.PromptTemplateView
import tri.promptfx.prompts.PromptLibraryView
import tri.promptfx.multimodal.ImagesView
import tri.promptfx.prompts.PromptTraceHistoryView
import tri.promptfx.ui.ImmersiveChatView
import tri.promptfx.ui.NavigableWorkspaceViewRuntime
import tri.util.ui.*
import tri.util.ui.starship.StarshipView
import tri.util.warning

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

    private fun Map<String, Map<String, PromptFxViewInfo>>.filter(predicate: (PromptFxViewInfo) -> Boolean) =
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
        
        // Track changes to the docked component to save last active view
        dockedComponentProperty.addListener { _, _, newComponent ->
            newComponent?.let { component ->
                // Find the view identifier for this component
                val viewIdentifier = findViewIdentifier(component)
                if (viewIdentifier != null) {
                    promptFxConfig.setLastActiveView(viewIdentifier)
                }
            }
        }
    }

    init {
        primaryStage.width = 1200.0
        primaryStage.height = 800.0
        with(leftDrawer) {
            group(ViewGroupModel("API", FontAwesomeIcon.CLOUD.graphic.steelBlue, listOf())) {
                (this as DrawerItem).padding = insets(5.0)
                hyperlinkview<ModelsView>("API", "Models")
                separator { }
                label("Text Completion API")
                hyperlinkview<CompletionsView>("API", "Completions")
                separator { }
                label("Chat APIs")
                hyperlinkview<ChatViewBasic>("API", "Chat")
                hyperlinkview<ChatViewAdvanced>("API", "Chat (Advanced)")
                separator { }
                label("Multimodal APIs")
                hyperlinkview<AudioView>("API", "Audio")
                hyperlinkview<AudioSpeechView>("API", "Speech")
                hyperlinkview<ImagesView>("API", "Images")
                separator { }
                label("Advanced APIs")
                hyperlinkview<EmbeddingsView>("API", "Embeddings")
//                hyperlinkview<FineTuningApiView>("API", "Fine-tuning")
//                hyperlinkview<FilesView>("API", "Files")
                hyperlinkview<ModerationsView>("API", "Moderations")
            }
            PromptFxWorkspaceModel.instance.viewGroups.forEach {
                // Skip API category as it's manually configured above
                if (it.category != "API") {
                    group(it)
                }
            }
        }
    }

    override fun onBeforeShow() {
        // Try to restore the last active view, fallback to DocumentQaView if not available
        val lastActiveViewIdentifier = promptFxConfig.getLastActiveView()
        if (lastActiveViewIdentifier != null) {
            val restored = tryRestoreView(lastActiveViewIdentifier)
            if (!restored) {
                // Fallback to default view if restore failed
                dock<DocumentQaView>()
            }
        } else {
            // First time run - use default view
            dock<DocumentQaView>()
        }
    }

    //region VIEW LOOKUPS

    /** Find the view identifier (category:name) for a given component. */
    private fun findViewIdentifier(component: UIComponent): String? {
        return views.entries.flatMap { categoryEntry ->
            categoryEntry.value.entries.map { viewEntry ->
                Triple(categoryEntry.key, viewEntry.key, viewEntry.value)
            }
        }.find { (_, _, viewInfo) ->
            val matchesComponent = viewInfo.viewComponent == component
            val matchesView = viewInfo.view != null && component.javaClass == viewInfo.view
            matchesComponent || matchesView
        }?.let { (category, name, _) ->
            "$category:$name"
        }
    }

    /** Attempts to restore a view by its identifier (category:name). Returns true if successful. */
    private fun tryRestoreView(viewIdentifier: String): Boolean {
        return try {
            val parts = viewIdentifier.split(":", limit = 2)
            if (parts.size != 2) return false
            
            val (category, name) = parts
            val viewInfo = views[category]?.get(name)
            
            if (viewInfo != null) {
                if (viewInfo.viewComponent != null) {
                    dock(viewInfo.viewComponent!!)
                    return true
                } else if (viewInfo.view != null) {
                    val viewInstance = find(viewInfo.view!!)
                    dock(viewInstance)
                    return true
                }
            }
            
            false
        } catch (e: Exception) {
            warning<PromptFxWorkspace>("Failed to restore view $viewIdentifier: ${e.message}")
            false
        }
    }

    //endregion

    //region HOOKS FOR SPECIFIC VIEWS

    /**
     * Looks up a view by name. This may instantiate a new view if not already created.
     */
    fun findTaskView(name: String): AiTaskView? {
        return views.values.map { it.entries }.flatten()
            .find { it.key == name }
            ?.let { it.value.viewComponent ?: find(it.value.view!!) } as? AiTaskView
    }

    /**
     * Looks up a view by type. This may instantiate a new view if not already created.
     */
    inline fun <reified T> findTaskView(): T? where T: AiTaskView {
        return views.values.map { it.entries }.flatten()
            .map { it.value.viewComponent ?: find(it.value.view!!) }
            .filterIsInstance<T>().firstOrNull()
    }

    /** Launches the template view with the given prompt trace. */
    fun launchHistoryView(prompt: AiPromptTraceSupport) {
        val view = find<PromptTraceHistoryView>()
        view.selectPromptTrace(prompt)
        workspace.dock(view)
    }

    /** Launches the template view with the given prompt trace. */
    fun launchTemplateView(prompt: AiPromptTraceSupport) {
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

    /** Launches the prompt library view. */
    fun launchLibraryView() {
        val view = find<PromptLibraryView>()
        workspace.dock(view)
    }

    /** Launches the prompt library view and selects the prompt matching the given template text. */
    fun launchLibraryView(templateText: String) {
        val view = find<PromptLibraryView>()
        workspace.dock(view)
        // Select the prompt after docking
        runLater {
            view.selectPromptByTemplate(templateText)
        }
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

    private fun Drawer.group(model: ViewGroupModel, op: EventTarget.() -> Unit = { }) {
        item(model.category, model.icon, expanded = false) {
            op()
            if (model.category == "Custom") {
                padding = insets(5.0)
                var isFirstCategory = true

                model.views.filter { it.category !in PromptFxWorkspaceModel.BUILT_IN_CATEGORIES }
                    .groupBy { it.category }.toSortedMap()
                    .forEach { cat, views ->
                        if (!isFirstCategory) separator { }
                        isFirstCategory = false
                        label(cat)
                        views.sortedBy { it.name }.forEach {
                            hyperlinkview(model.category, it)
                        }
                    }
            } else {
                model.views.forEach {
                    hyperlinkview(it.category, it)
                }
            }
            // Automatically load view links for this category
            loadViewLinks(model.category)
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

    private fun EventTarget.hyperlinkview(viewGroup: String, view: NavigableWorkspaceView) {
        val viewSimpleName = (view as? NavigableWorkspaceViewImpl<*>)?.type?.simpleName ?: view.name
        if (PromptFxModels.policy.supportsView(viewSimpleName)) {
            val viewInfo = view.viewInfo()
            if (viewInfo != null)
                views.getOrPut(viewGroup) { mutableMapOf() }.put(view.name, viewInfo)
            hyperlink(view.name) {
                action {
                    isVisited = false
                    view.dock(this@PromptFxWorkspace)
                }
            }
        }
    }

    private fun NavigableWorkspaceView.viewInfo() = when (this) {
        is NavigableWorkspaceViewImpl<*> -> PromptFxViewInfo(category, name, type.java, null, affordances)
        is NavigableWorkspaceViewRuntime -> PromptFxViewInfo(category, name, null, view, affordances)
        else -> null
    }

    private fun EventTarget.browsehyperlink(label: String, url: String) {
        hyperlink(label, graphic = FontAwesomeIcon.EXTERNAL_LINK.graphic) {
            action { hostServices.showDocument(url) }
        }
    }

    private fun EventTarget.loadViewLinks(category: String) {
        val categoryLinks = ViewLinksConfig.links[category] ?: return
        if (categoryLinks.isEmpty()) return
        
        separator { }
        label("Documentation/Links")
        
        categoryLinks.forEachIndexed { index, linkGroup ->
            if (linkGroup.links.isNotEmpty()) {
                // Add separator between link groups (but not before the very first group)
                if (index > 0) {
                    separator { }
                }
                linkGroup.links.forEach { link ->
                    browsehyperlink(link.label, link.url)
                }
            }
        }
    }

    //endregion

}

