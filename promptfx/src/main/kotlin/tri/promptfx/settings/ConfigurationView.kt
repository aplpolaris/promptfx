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
package tri.promptfx.settings

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import javafx.util.Callback
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxWorkspaceModel
import tri.promptfx.RuntimePromptViewConfigs
import tri.promptfx.PromptFxConfig
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import tri.util.ui.starship.StarshipContentConfig
import java.io.File

/** Plugin for the [ConfigurationView]. */
class ConfigurationPlugin : NavigableWorkspaceViewImpl<ConfigurationView>("Settings", "Configuration", type = ConfigurationView::class)

/** Configuration view showing application configuration and runtime information. */
class ConfigurationView : AiTaskView("Application Configuration", "View and manage application configuration settings.", showInput = false) {

    private var detailPane: ScrollPane by singleAssign()
    
    init {
        hideParameters()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    init {
        outputPane.clear()
        output {
            vgrow = Priority.ALWAYS

            splitpane(Orientation.HORIZONTAL) {
                // Left panel - Tree navigation
                vbox {
                    label("Configuration Categories") {
                        style {
                            fontSize = 14.px
                            fontWeight = FontWeight.BOLD
                        }
                    }
                    separator()
                    
                    treeview<ConfigCategory> {
                        vgrow = Priority.ALWAYS
                        prefWidth = 250.0
                        
                        root = TreeItem(ConfigCategory.ROOT)
                        root.isExpanded = true
                        
                        // Add category items
                        root.children.addAll(
                            TreeItem(ConfigCategory.RUNTIME),
                            TreeItem(ConfigCategory.APIS_MODELS),
                            TreeItem(ConfigCategory.VIEWS),
                            TreeItem(ConfigCategory.CONFIG_FILES),
                            TreeItem(ConfigCategory.PROMPTFX_CONFIG),
                            TreeItem(ConfigCategory.STARSHIP_CONFIG)
                        )
                        
                        cellFactory = Callback { 
                            object : TreeCell<ConfigCategory>() {
                                override fun updateItem(item: ConfigCategory?, empty: Boolean) {
                                    super.updateItem(item, empty)
                                    if (empty || item == null) {
                                        text = null
                                        graphic = null
                                    } else {
                                        text = item.displayName
                                        graphic = item.icon.graphic
                                    }
                                }
                            }
                        }
                        
                        // Handle selection changes
                        selectionModel.selectedItemProperty().addListener { _, _, newItem ->
                            newItem?.let { showCategoryDetails(it.value) }
                        }
                        
                        // Select runtime by default
                        selectionModel.select(1)
                    }
                }
                
                // Right panel - Details
                detailPane = scrollpane {
                    vgrow = Priority.ALWAYS
                    hgrow = Priority.ALWAYS
                    
                    vbox {
                        paddingAll = 20.0
                        label("Select a category from the tree to view configuration details.") {
                            style {
                                fontSize = 14.px
                            }
                        }
                    }
                }
                
                setDividerPosition(0, 0.3)
            }
        }
    }
    
    private fun showCategoryDetails(category: ConfigCategory) {
        when (category) {
            ConfigCategory.RUNTIME -> showRuntimeDetails()
            ConfigCategory.APIS_MODELS -> showApisModelsDetails()
            ConfigCategory.VIEWS -> showViewsDetails()
            ConfigCategory.CONFIG_FILES -> showConfigFilesDetails()
            ConfigCategory.PROMPTFX_CONFIG -> showPromptFxConfigDetails()
            ConfigCategory.STARSHIP_CONFIG -> showStarshipConfigDetails()
            else -> {
                detailPane.content = vbox {
                    paddingAll = 20.0
                    label("Select a specific category to view details.") {
                        style {
                            fontSize = 14.px
                        }
                    }
                }
            }
        }
    }
    
    private fun showRuntimeDetails() {
        detailPane.content = vbox(10) {
            paddingAll = 20.0
            
            label("Global Runtime Objects") {
                style {
                    fontSize = 16.px
                    fontWeight = FontWeight.BOLD
                }
            }
            
            // Completion Engine
            vbox(5) {
                label("Text Completion Engine:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label(controller.completionEngine.stringBinding { "Model: ${it?.modelId ?: "Not set"}" })
                label(controller.completionEngine.stringBinding { "Provider: ${it?.javaClass?.simpleName ?: "Unknown"}" })
            }
            
            separator()
            
            // Chat Service
            vbox(5) {
                label("Chat Service:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label(controller.chatService.stringBinding { "Model: ${it?.modelId ?: "Not set"}" })
                label(controller.chatService.stringBinding { "Provider: ${it?.javaClass?.simpleName ?: "Unknown"}" })
            }
            
            separator()
            
            // Embedding Strategy
            vbox(5) {
                label("Embedding Strategy:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label(controller.embeddingStrategy.stringBinding { "Model: ${it?.model?.modelId ?: "Not set"}" })
                label(controller.embeddingStrategy.stringBinding { "Chunker: ${it?.chunker?.javaClass?.simpleName ?: "Unknown"}" })
            }
            
            separator()
            
            // Usage Statistics
            vbox(5) {
                label("Usage Statistics:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label(controller.tokensUsed.stringBinding { "Tokens Used: $it" })
                label(controller.audioUsed.stringBinding { "Audio Usage: $it" })
                label(controller.imagesUsed.stringBinding { "Images Used: $it" })
            }
            
            separator()
            
            // Current Policy
            vbox(5) {
                label("Current Policy:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Policy: ${PromptFxModels.policy.javaClass.simpleName}")
                if (PromptFxModels.policy.isShowBanner) {
                    label("Banner: ${PromptFxModels.policy.bar.text}")
                }
            }
        }
    }
    
    private fun showApisModelsDetails() {
        detailPane.content = vbox(10) {
            paddingAll = 20.0
            
            label("Model Policy & Plugins") {
                style {
                    fontSize = 16.px
                    fontWeight = FontWeight.BOLD
                }
            }
            
            // Current Model Policy
            vbox(5) {
                label("Current Model Policy:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Policy Type: ${PromptFxModels.policy.javaClass.simpleName}")
                label("Show Banner: ${PromptFxModels.policy.isShowBanner}")
            }
            
            separator()
            
            // Default Models
            vbox(5) {
                label("Default Models:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Text Completion: ${PromptFxModels.textCompletionModelDefault()?.modelId}")
                label("Chat Model: ${PromptFxModels.chatModelDefault()?.modelId}")
                label("Embedding Model: ${PromptFxModels.embeddingModelDefault()?.modelId}")
            }
            
            separator()
            
            // Policy Plugins
            vbox(5) {
                label("Plugins Active in Policy:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val policyPlugins = PromptFxModels.policy.supportedPlugins()
                policyPlugins.forEach { plugin ->
                    val modelCount = plugin.modelInfo().size
                    label("• ${plugin.javaClass.simpleName} ($modelCount models)")
                }
            }
            
            separator()
            
            // All Discovered Plugins
            vbox(5) {
                label("All Discovered Plugins:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val allPlugins = TextPlugin.orderedPlugins
                val policyPlugins = PromptFxModels.policy.supportedPlugins()
                allPlugins.forEach { plugin ->
                    val modelCount = plugin.modelInfo().size
                    val status = if (plugin in policyPlugins) " (Active)" else " (Available)"
                    label("• ${plugin.javaClass.simpleName} ($modelCount models)$status")
                }
            }
            
            separator()
            
            // Available Models
            vbox(5) {
                label("Available Models:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val allModels = PromptFxModels.policy.modelInfo()
                if (allModels.isNotEmpty()) {
                    allModels.take(10).forEach { model ->
                        label("• ${model.id} (${model.javaClass.simpleName})")
                    }
                    if (allModels.size > 10) {
                        label("... and ${allModels.size - 10} more models")
                    }
                } else {
                    label("No models available")
                }
            }
        }
    }
    
    private fun showViewsDetails() {
        detailPane.content = vbox(10) {
            paddingAll = 20.0
            
            label("Application Views") {
                style {
                    fontSize = 16.px
                    fontWeight = FontWeight.BOLD
                }
            }
            
            // Categorize views by type
            val builtInViews = mutableListOf<NavigableWorkspaceView>()
            val runtimeViews = mutableListOf<NavigableWorkspaceView>()  
            val customizedViews = mutableListOf<NavigableWorkspaceView>()
            
            PromptFxWorkspaceModel.instance.viewGroups.forEach { group ->
                group.views.forEach { view ->
                    when {
                        view is tri.promptfx.ui.NavigableWorkspaceViewRuntime -> runtimeViews.add(view)
                        // Check if it's a built-in view that has been customized/overridden at runtime
                        NavigableWorkspaceView.viewPlugins.any { plugin -> 
                            plugin.name == view.name && RuntimePromptViewConfigs.views.values.any { config ->
                                config.prompt.title == view.name || config.prompt.name == view.name
                            }
                        } -> customizedViews.add(view)
                        else -> builtInViews.add(view)
                    }
                }
            }
            
            // Built-in Views
            vbox(5) {
                label("Built-in Views (${builtInViews.size}):") {
                    style { fontWeight = FontWeight.BOLD }
                }
                builtInViews.groupBy { it.category }.forEach { (category, views) ->
                    label("  $category:")
                    views.forEach { view ->
                        label("    • ${view.name}")
                    }
                }
                if (builtInViews.isEmpty()) {
                    label("  No built-in views")
                }
            }
            
            separator()
            
            // Runtime Views
            vbox(5) {
                label("Runtime Views (${runtimeViews.size}):") {
                    style { fontWeight = FontWeight.BOLD }
                }
                runtimeViews.groupBy { it.category }.forEach { (category, views) ->
                    label("  $category:")
                    views.forEach { view ->
                        label("    • ${view.name}")
                    }
                }
                if (runtimeViews.isEmpty()) {
                    label("  No runtime views")
                }
            }
            
            separator()
            
            // Customized Views
            vbox(5) {
                label("Customized Views (${customizedViews.size}):") {
                    style { fontWeight = FontWeight.BOLD }
                }
                customizedViews.groupBy { it.category }.forEach { (category, views) ->
                    label("  $category:")
                    views.forEach { view ->
                        label("    • ${view.name} (Built-in overridden at runtime)")
                    }
                }
                if (customizedViews.isEmpty()) {
                    label("  No customized views")
                }
            }
            
            separator()
            
            // View Plugins
            vbox(5) {
                label("Registered View Plugins:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                NavigableWorkspaceView.viewPlugins.forEach { plugin ->
                    label("• ${plugin.name} (${plugin.category})")
                }
            }
            
            separator()
            
            // Runtime View Configs
            vbox(5) {
                label("Runtime View Configurations:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                RuntimePromptViewConfigs.views.values.forEach { config ->
                    label("• ${config.prompt.title ?: config.prompt.name ?: config.prompt.id} (${config.prompt.category ?: "Uncategorized"})")
                }
                if (RuntimePromptViewConfigs.views.isEmpty()) {
                    label("No runtime view configurations found")
                }
            }
        }
    }
    
    private fun showConfigFilesDetails() {
        detailPane.content = vbox(10) {
            paddingAll = 20.0
            
            label("Configuration Files") {
                style {
                    fontSize = 16.px
                    fontWeight = FontWeight.BOLD
                }
            }
            
            // Config Directory
            vbox(5) {
                label("Config Directory:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val configDir = File("config")
                label("Path: ${configDir.absolutePath}")
                label("Exists: ${configDir.exists()}")
                
                if (configDir.exists() && configDir.isDirectory) {
                    label("Contents:")
                    configDir.listFiles()?.forEach { file ->
                        label("  • ${file.name} (${if (file.isDirectory) "directory" else "file, ${file.length()} bytes"})")
                    }
                }
            }
            
            separator()
            
            // YAML Configuration Files
            vbox(5) {
                label("YAML Configuration Files:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                
                val yamlFiles = listOf(
                    File("views.yaml"),
                    File("modes.yaml"),
                    File("config/views.yaml"),
                    File("config/modes.yaml")
                )
                
                yamlFiles.forEach { file ->
                    label("• ${file.path}: ${if (file.exists()) "Found (${file.length()} bytes)" else "Not found"}")
                }
            }
            
            separator()
            
            // Prompt Library Files
            vbox(5) {
                label("Prompt Library:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val promptsDir = File("prompts")
                label("Prompts Directory: ${promptsDir.absolutePath}")
                label("Exists: ${promptsDir.exists()}")
                
                if (promptsDir.exists() && promptsDir.isDirectory) {
                    val yamlFiles = promptsDir.listFiles { _, name -> name.endsWith(".yaml") || name.endsWith(".yml") }
                    if (yamlFiles != null && yamlFiles.isNotEmpty()) {
                        label("Prompt YAML files:")
                        yamlFiles.forEach { file ->
                            label("  • ${file.name} (${file.length()} bytes)")
                        }
                    } else {
                        label("No YAML files found in prompts directory")
                    }
                }
            }
            
            separator()
            
            // System Properties
            vbox(5) {
                label("System Properties (relevant):") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val relevantProps = listOf(
                    "user.dir",
                    "user.home",
                    "java.version",
                    "java.vendor",
                    "os.name",
                    "os.version"
                )
                relevantProps.forEach { prop ->
                    label("• $prop: ${System.getProperty(prop)}")
                }
            }
        }
    }
    
    private fun showPromptFxConfigDetails() {
        detailPane.content = vbox(10) {
            paddingAll = 20.0
            
            label("PromptFx Configuration") {
                style {
                    fontSize = 16.px
                    fontWeight = FontWeight.BOLD
                }
            }
            
            val promptFxConfig = find<PromptFxConfig>()
            
            // Basic settings
            vbox(5) {
                label("Basic Settings:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Starship Enabled: ${promptFxConfig.isStarshipEnabled}")
            }
            
            separator()
            
            // Directory Management
            vbox(5) {
                label("Directory Management:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val dirKeys = listOf(
                    "default", 
                    PromptFxConfig.DIR_KEY_TEXTLIB,
                    PromptFxConfig.DIR_KEY_TXT,
                    PromptFxConfig.DIR_KEY_TRACE,
                    PromptFxConfig.DIR_KEY_IMAGE
                )
                dirKeys.forEach { key ->
                    val dir = promptFxConfig.directory(key)
                    val file = promptFxConfig.directoryFile(key)
                    label("• $key: ${dir.absolutePath}" + (file?.let { " / $it" } ?: ""))
                }
            }
            
            separator()
            
            // Library Files
            vbox(5) {
                label("Library Files:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val textManagerFiles = promptFxConfig.textManagerFiles()
                label("Text Manager Files (${textManagerFiles.size}):")
                textManagerFiles.forEach { file ->
                    label("  • ${file.name} (${if (file.exists()) "${file.length()} bytes" else "missing"})")
                }
                
                val textClusterFiles = promptFxConfig.textClusterFiles()
                label("Text Cluster Files (${textClusterFiles.size}):")
                textClusterFiles.forEach { file ->
                    label("  • ${file.name} (${if (file.exists()) "${file.length()} bytes" else "missing"})")
                }
            }
            
            separator()
            
            // File Extension Filters
            vbox(5) {
                label("Supported File Types:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("• CSV Files: ${PromptFxConfig.FF_CSV.extensions.joinToString(", ")}")
                label("• JSON Files: ${PromptFxConfig.FF_JSON.extensions.joinToString(", ")}")
                label("• YAML Files: ${PromptFxConfig.FF_YAML.extensions.joinToString(", ")}")
                label("• Text Files: ${PromptFxConfig.FF_TXT.extensions.joinToString(", ")}")
                label("• Image Files: ${PromptFxConfig.FF_IMAGE.extensions.joinToString(", ")}")
            }
        }
    }
    
    private fun showStarshipConfigDetails() {
        detailPane.content = vbox(10) {
            paddingAll = 20.0
            
            label("Starship Demo Configuration") {
                style {
                    fontSize = 16.px
                    fontWeight = FontWeight.BOLD
                }
            }
            
            // Basic Settings
            vbox(5) {
                label("Display Settings:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Background Icon: ${StarshipContentConfig.backgroundIcon}")
                label("Background Icon Count: ${StarshipContentConfig.backgroundIconCount}")
                label("Show Grid: ${StarshipContentConfig.isShowGrid}")
            }
            
            separator()
            
            // Explanation Text
            vbox(5) {
                label("Explanation Steps (${StarshipContentConfig.explain.size}):") {
                    style { fontWeight = FontWeight.BOLD }
                }
                StarshipContentConfig.explain.forEachIndexed { index, step ->
                    label("${index + 1}. $step")
                }
            }
            
            separator()
            
            // Prompt Information
            vbox(5) {
                label("Prompt Pipeline Configuration:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                StarshipContentConfig.promptInfo.forEach { info ->
                    when (info) {
                        is String -> label("• $info")
                        is Map<*, *> -> {
                            info.entries.forEach { (key, value) ->
                                label("• $key:")
                                if (value is Map<*, *>) {
                                    value.entries.forEach { (subKey, subValue) ->
                                        label("    $subKey: $subValue")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            separator()
            
            // User Options
            vbox(5) {
                label("User Options:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                StarshipContentConfig.userOptions.forEach { (promptId, options) ->
                    label("• $promptId:")
                    options.forEach { (optionKey, optionValues) ->
                        label("    $optionKey (${optionValues.size} options): ${optionValues.take(3).joinToString(", ")}${if (optionValues.size > 3) "..." else ""}")
                    }
                }
            }
        }
    }

    override suspend fun processUserInput() = TODO("Configuration view does not process user input")
}

/** Categories for configuration tree navigation. */
enum class ConfigCategory(val displayName: String, val icon: FontAwesomeIcon) {
    ROOT("Configuration", FontAwesomeIcon.COG),
    RUNTIME("Runtime", FontAwesomeIcon.GEARS),
    APIS_MODELS("APIs/Models", FontAwesomeIcon.CLOUD),
    VIEWS("Views", FontAwesomeIcon.SITEMAP),
    CONFIG_FILES("Configuration Files", FontAwesomeIcon.FILE_ALT),
    PROMPTFX_CONFIG("PromptFx Config", FontAwesomeIcon.WRENCH),
    STARSHIP_CONFIG("Starship Config", FontAwesomeIcon.ROCKET)
}