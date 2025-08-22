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
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import java.io.File

/** Plugin for the [ConfigurationView]. */
class ConfigurationPlugin : NavigableWorkspaceViewImpl<ConfigurationView>("Settings", "Configuration", type = ConfigurationView::class)

/** Configuration view showing application configuration and runtime information. */
class ConfigurationView : AiTaskView("Application Configuration", "View and manage application configuration settings.", showInput = false) {

    private var detailPane: TabPane by singleAssign()
    
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
                            TreeItem(ConfigCategory.CONFIG_FILES)
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
                detailPane = tabpane {
                    vgrow = Priority.ALWAYS
                    hgrow = Priority.ALWAYS
                    
                    tab("Select Category") {
                        isClosable = false
                        label("Select a category from the tree to view configuration details.")
                    }
                }
                
                setDividerPosition(0, 0.3)
            }
        }
    }
    
    private fun showCategoryDetails(category: ConfigCategory) {
        detailPane.tabs.clear()
        
        when (category) {
            ConfigCategory.RUNTIME -> showRuntimeDetails()
            ConfigCategory.APIS_MODELS -> showApisModelsDetails()
            ConfigCategory.VIEWS -> showViewsDetails()
            ConfigCategory.CONFIG_FILES -> showConfigFilesDetails()
            else -> {
                detailPane.tabs.add(Tab("Select Category").apply {
                    isClosable = false
                    content = label("Select a specific category to view details.")
                })
            }
        }
    }
    
    private fun showRuntimeDetails() {
        detailPane.tabs.add(Tab("Runtime Configuration").apply {
            isClosable = false
            content = scrollpane {
                vbox(10) {
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
                        label("Model: ${controller.completionEngine.value?.modelId ?: "Not set"}")
                        label("Provider: ${controller.completionEngine.value?.javaClass?.simpleName ?: "Unknown"}")
                    }
                    
                    separator()
                    
                    // Chat Service
                    vbox(5) {
                        label("Chat Service:") {
                            style { fontWeight = FontWeight.BOLD }
                        }
                        label("Model: ${controller.chatService.value?.modelId ?: "Not set"}")
                        label("Provider: ${controller.chatService.value?.javaClass?.simpleName ?: "Unknown"}")
                    }
                    
                    separator()
                    
                    // Embedding Strategy
                    vbox(5) {
                        label("Embedding Strategy:") {
                            style { fontWeight = FontWeight.BOLD }
                        }
                        label("Model: ${controller.embeddingStrategy.value?.model?.modelId ?: "Not set"}")
                        label("Chunker: ${controller.embeddingStrategy.value?.chunker?.javaClass?.simpleName ?: "Unknown"}")
                    }
                    
                    separator()
                    
                    // Usage Statistics
                    vbox(5) {
                        label("Usage Statistics:") {
                            style { fontWeight = FontWeight.BOLD }
                        }
                        label("Tokens Used: ${controller.tokensUsed.value}")
                        label("Audio Usage: ${controller.audioUsed.value}")
                        label("Images Used: ${controller.imagesUsed.value}")
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
        })
    }
    
    private fun showApisModelsDetails() {
        detailPane.tabs.add(Tab("APIs & Models").apply {
            isClosable = false
            content = scrollpane {
                vbox(10) {
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
                    
                    // Text Plugins
                    vbox(5) {
                        label("Discovered Text Plugins:") {
                            style { fontWeight = FontWeight.BOLD }
                        }
                        val defaultPlugin = controller.openAiPlugin
                        val modelCount = defaultPlugin.modelInfo().size
                        label("• ${defaultPlugin.javaClass.simpleName} ($modelCount models)")
                    }
                    
                    separator()
                    
                    // Available Models
                    vbox(5) {
                        label("Available Models:") {
                            style { fontWeight = FontWeight.BOLD }
                        }
                        val allModels = controller.openAiPlugin.modelInfo()
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
        })
    }
    
    private fun showViewsDetails() {
        detailPane.tabs.add(Tab("Views Configuration").apply {
            isClosable = false
            content = scrollpane {
                vbox(10) {
                    paddingAll = 20.0
                    
                    label("Application Views") {
                        style {
                            fontSize = 16.px
                            fontWeight = FontWeight.BOLD
                        }
                    }
                    
                    // View Groups
                    PromptFxWorkspaceModel.instance.viewGroups.forEach { group ->
                        vbox(5) {
                            label("${group.category} Views:") {
                                style { fontWeight = FontWeight.BOLD }
                            }
                            group.views.forEach { view ->
                                val isRuntimeView = view is tri.promptfx.ui.NavigableWorkspaceViewRuntime
                                val indicator = if (isRuntimeView) " (Runtime)" else " (Built-in)"
                                label("  • ${view.name}$indicator")
                            }
                            if (group.views.isEmpty()) {
                                label("  No views in this category")
                            }
                        }
                        separator()
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
        })
    }
    
    private fun showConfigFilesDetails() {
        detailPane.tabs.add(Tab("Configuration Files").apply {
            isClosable = false
            content = scrollpane {
                vbox(10) {
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
        })
    }

    override suspend fun processUserInput() = TODO("Configuration view does not process user input")
}

/** Categories for configuration tree navigation. */
enum class ConfigCategory(val displayName: String, val icon: FontAwesomeIcon) {
    ROOT("Configuration", FontAwesomeIcon.COG),
    RUNTIME("Runtime", FontAwesomeIcon.GEARS),
    APIS_MODELS("APIs/Models", FontAwesomeIcon.CLOUD),
    VIEWS("Views", FontAwesomeIcon.SITEMAP),
    CONFIG_FILES("Configuration Files", FontAwesomeIcon.FILE_ALT)
}