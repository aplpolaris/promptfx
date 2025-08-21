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
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxWorkspaceModel
import tri.promptfx.RuntimePromptViewConfigs
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.graphic
import java.io.File

/** Application configuration view showing runtime configuration and settings. */
class ConfigurationView : AiTaskView("Configuration", "Application configuration and runtime information.", showInput = false) {

    // controller is already available from parent AiTaskView
    
    private val selectedCategory = SimpleStringProperty("")
    private val selectedItem = SimpleStringProperty("")
    
    // Tree structure for navigation
    private lateinit var configTree: TreeView<ConfigItem>
    
    // Detail pane content
    private val detailContent = SimpleStringProperty("Select a category to view configuration details.")

    data class ConfigItem(
        val name: String,
        val type: String,
        val value: Any? = null
    )

    init {
        hideParameters()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    init {
        outputPane.clear()
        
        output {
            vgrow = Priority.ALWAYS
            
            splitpane {
                // Left side - Navigation Tree
                vbox {
                    prefWidth = 300.0
                    minWidth = 250.0
                    
                    label("Configuration Categories") {
                        style {
                            fontSize = 16.px
                            fontWeight = FontWeight.BOLD
                            padding = box(10.px)
                        }
                    }
                    
                    configTree = treeview<ConfigItem> {
                        vgrow = Priority.ALWAYS
                        
                        root = TreeItem(ConfigItem("Configuration", "root"))
                        root.isExpanded = true
                        isShowRoot = false
                        
                        // Build tree structure
                        buildConfigTree()
                        
                        // Handle selection changes
                        selectionModel.selectedItemProperty().onChange { selectedTreeItem ->
                            selectedTreeItem?.let { item ->
                                selectedCategory.set(item.value.type)
                                selectedItem.set(item.value.name)
                                updateDetailContent(item.value)
                            }
                        }
                    }
                }
                
                // Right side - Detail View
                scrollpane {
                    prefWidth = 500.0
                    
                    vbox {
                        padding = insets(20.0)
                        spacing = 10.0
                        
                        label {
                            textProperty().bind(selectedItem)
                            style {
                                fontSize = 18.px
                                fontWeight = FontWeight.BOLD
                            }
                        }
                        
                        text {
                            textProperty().bind(detailContent)
                            wrappingWidth = 480.0
                            style {
                                fontSize = 12.px
                            }
                        }
                    }
                }
            }
        }
    }

    private fun TreeView<ConfigItem>.buildConfigTree() {
        // Runtime category
        val runtimeItem = TreeItem(ConfigItem("Runtime", "category"))
        runtimeItem.children.addAll(listOf(
            TreeItem(ConfigItem("Current Policy", "runtime_policy")),
            TreeItem(ConfigItem("Default Models", "runtime_models")),
            TreeItem(ConfigItem("Usage Statistics", "runtime_usage")),
            TreeItem(ConfigItem("Global Objects", "runtime_globals"))
        ))
        
        // APIs/Models category
        val modelsItem = TreeItem(ConfigItem("APIs/Models", "category"))
        modelsItem.children.addAll(listOf(
            TreeItem(ConfigItem("Model Policy", "models_policy")),
            TreeItem(ConfigItem("Available Plugins", "models_plugins")),
            TreeItem(ConfigItem("Active Plugins", "models_active")),
            TreeItem(ConfigItem("Plugin Configuration", "models_config"))
        ))
        
        // Views category
        val viewsItem = TreeItem(ConfigItem("Views", "category"))
        viewsItem.children.addAll(listOf(
            TreeItem(ConfigItem("View Structure", "views_structure")),
            TreeItem(ConfigItem("Runtime Views", "views_runtime")),
            TreeItem(ConfigItem("Built-in Views", "views_builtin"))
        ))
        
        // Configuration Files category
        val filesItem = TreeItem(ConfigItem("Configuration Files", "category"))
        filesItem.children.addAll(listOf(
            TreeItem(ConfigItem("Config Directory", "files_config")),
            TreeItem(ConfigItem("Prompt Libraries", "files_prompts")),
            TreeItem(ConfigItem("YAML Files", "files_yaml"))
        ))
        
        root.children.addAll(listOf(runtimeItem, modelsItem, viewsItem, filesItem))
        runtimeItem.isExpanded = true
    }

    private fun updateDetailContent(item: ConfigItem) {
        val content = when (item.type) {
            "runtime_policy" -> buildRuntimePolicyInfo()
            "runtime_models" -> buildRuntimeModelsInfo()
            "runtime_usage" -> buildRuntimeUsageInfo()
            "runtime_globals" -> buildRuntimeGlobalsInfo()
            "models_policy" -> buildModelsPolicyInfo()
            "models_plugins" -> buildModelsPluginsInfo()
            "models_active" -> buildModelsActiveInfo()
            "models_config" -> buildModelsConfigInfo()
            "views_structure" -> buildViewsStructureInfo()
            "views_runtime" -> buildViewsRuntimeInfo()
            "views_builtin" -> buildViewsBuiltinInfo()
            "files_config" -> buildFilesConfigInfo()
            "files_prompts" -> buildFilesPromptsInfo()
            "files_yaml" -> buildFilesYamlInfo()
            else -> "Select a configuration item to view details."
        }
        detailContent.set(content)
    }

    private fun buildRuntimePolicyInfo(): String {
        val policy = PromptFxModels.policy
        return buildString {
            appendLine("Current Policy Information:")
            appendLine("==========================")
            appendLine()
            appendLine("Policy Class: ${policy::class.simpleName}")
            appendLine("Policy Type: ${policy.javaClass.name}")
            appendLine()
            
            val modelInfo = try { policy.modelInfo() } catch (e: Exception) { emptyList() }
            appendLine("Available Model Types: ${modelInfo.size}")
            modelInfo.forEach { model ->
                appendLine("  - ${model.id} (${model.type})")
            }
        }
    }

    private fun buildRuntimeModelsInfo(): String {
        return buildString {
            appendLine("Default Model Configuration:")
            appendLine("============================")
            appendLine()
            
            appendLine("Text Completion:")
            appendLine("  Engine: ${controller.completionEngine.value?.modelId ?: "None"}")
            appendLine()
            
            appendLine("Chat Service:")
            appendLine("  Service: ${controller.chatService.value?.modelId ?: "None"}")
            appendLine()
            
            appendLine("Embedding Strategy:")
            appendLine("  Model: ${controller.embeddingStrategy.value?.model?.modelId ?: "None"}")
            appendLine("  Chunker: ${controller.embeddingStrategy.value?.chunker?.javaClass?.simpleName ?: "None"}")
        }
    }

    private fun buildRuntimeUsageInfo(): String {
        controller.updateUsage() // Refresh usage stats
        
        return buildString {
            appendLine("API Usage Statistics:")
            appendLine("=====================")
            appendLine()
            appendLine("OpenAI Plugin Usage:")
            appendLine("  Tokens Used: ${controller.tokensUsed.value}")
            appendLine("  Audio Seconds: ${controller.audioUsed.value}")
            appendLine("  Images Generated: ${controller.imagesUsed.value}")
            appendLine()
            
            val pluginUsage = controller.openAiPlugin.client.usage
            if (pluginUsage.isNotEmpty()) {
                appendLine("Detailed Plugin Usage:")
                pluginUsage.forEach { (unit, count) ->
                    appendLine("  $unit: $count")
                }
            }
        }
    }

    private fun buildRuntimeGlobalsInfo(): String {
        return buildString {
            appendLine("Global Objects and Controllers:")
            appendLine("===============================")
            appendLine()
            appendLine("PromptFx Controller:")
            appendLine("  OpenAI Plugin: ${controller.openAiPlugin.javaClass.simpleName}")
            appendLine("  Prompt History Size: ${controller.promptHistory.prompts.size}")
            appendLine()
            appendLine("Workspace Model:")
            appendLine("  View Groups: ${PromptFxWorkspaceModel.instance.viewGroups.size}")
            appendLine("  Categories: ${PromptFxWorkspaceModel.instance.viewGroups.map { it.category }.joinToString(", ")}")
        }
    }

    private fun buildModelsPolicyInfo(): String {
        val policy = PromptFxModels.policy
        return buildString {
            appendLine("Model Policy Details:")
            appendLine("=====================")
            appendLine()
            appendLine("Text Completion Models: ${policy.textCompletionModels().size}")
            policy.textCompletionModels().forEach { model ->
                appendLine("  - ${model.modelId}")
            }
            appendLine()
            
            appendLine("Chat Models: ${policy.chatModels().size}")
            policy.chatModels().forEach { model ->
                appendLine("  - ${model.modelId}")
            }
            appendLine()
            
            appendLine("Embedding Models: ${policy.embeddingModels().size}")
            policy.embeddingModels().forEach { model ->
                appendLine("  - ${model.modelId}")
            }
            appendLine()
            
            appendLine("Multimodal Models: ${policy.multimodalModels().size}")
            policy.multimodalModels().forEach { model ->
                appendLine("  - ${model.modelId}")
            }
            appendLine()
            
            appendLine("Vision Language Models: ${policy.visionLanguageModels().size}")
            policy.visionLanguageModels().forEach { model ->
                appendLine("  - ${model.modelId}")
            }
            appendLine()
            
            appendLine("Image Models: ${policy.imageModels().size}")
            policy.imageModels().forEach { model ->
                appendLine("  - ${model.modelId}")
            }
        }
    }

    private fun buildModelsPluginsInfo(): String {
        val plugins = TextPlugin.orderedPlugins
        return buildString {
            appendLine("Discovered Model Plugins:")
            appendLine("=========================")
            appendLine()
            appendLine("Total Plugins: ${plugins.size}")
            appendLine()
            
            plugins.forEach { plugin ->
                appendLine("Plugin: ${plugin.javaClass.simpleName}")
                appendLine("  Class: ${plugin.javaClass.name}")
                appendLine("  Status: Available")
                appendLine()
            }
        }
    }

    private fun buildModelsActiveInfo(): String {
        val policy = PromptFxModels.policy
        return buildString {
            appendLine("Active Plugin Status:")
            appendLine("=====================")
            appendLine()
            appendLine("Current Policy: ${policy.javaClass.simpleName}")
            appendLine()
            
            val modelIds = PromptFxModels.modelIds()
            appendLine("Active Model IDs (${modelIds.size}):")
            modelIds.forEach { modelId: String ->
                appendLine("  - $modelId")
            }
        }
    }

    private fun buildModelsConfigInfo(): String {
        return buildString {
            appendLine("Plugin Configuration Settings:")
            appendLine("==============================")
            appendLine()
            appendLine("OpenAI Configuration:")
            appendLine("  Default Plugin: ${controller.openAiPlugin.javaClass.simpleName}")
            appendLine("  Client Status: ${if (controller.openAiPlugin.client != null) "Connected" else "Not Connected"}")
            appendLine()
            appendLine("Note: API keys and sensitive configuration details are not displayed for security.")
        }
    }

    private fun buildViewsStructureInfo(): String {
        val workspace = PromptFxWorkspaceModel.instance
        return buildString {
            appendLine("View Structure (as printed at startup):")
            appendLine("========================================")
            appendLine()
            
            workspace.viewGroups.forEach { group ->
                appendLine("${group.category}: ${group.views.map { it.name }.joinToString(", ")}")
                group.views.forEach { view ->
                    appendLine("  - ${view.name} (${view.javaClass.simpleName})")
                }
                appendLine()
            }
        }
    }

    private fun buildViewsRuntimeInfo(): String {
        val runtimeViews = RuntimePromptViewConfigs.views
        return buildString {
            appendLine("Runtime View Configurations:")
            appendLine("============================")
            appendLine()
            appendLine("Runtime Views: ${runtimeViews.size}")
            appendLine()
            
            runtimeViews.forEach { (id, viewConfig) ->
                appendLine("View ID: $id")
                appendLine("  Title: ${viewConfig.prompt.title ?: "N/A"}")
                appendLine("  Name: ${viewConfig.prompt.name ?: "N/A"}")
                appendLine("  Category: ${viewConfig.prompt.category ?: "Uncategorized"}")
                appendLine("  Description: ${viewConfig.prompt.description ?: "N/A"}")
                appendLine()
            }
        }
    }

    private fun buildViewsBuiltinInfo(): String {
        val builtinViews = NavigableWorkspaceView.viewPlugins
        return buildString {
            appendLine("Built-in View Plugins:")
            appendLine("======================")
            appendLine()
            appendLine("Total Built-in Views: ${builtinViews.size}")
            appendLine()
            
            builtinViews.groupBy { it.category }.forEach { (category, views) ->
                appendLine("$category (${views.size} views):")
                views.forEach { view ->
                    appendLine("  - ${view.name}")
                }
                appendLine()
            }
        }
    }

    private fun buildFilesConfigInfo(): String {
        val configDir = File("config")
        return buildString {
            appendLine("Configuration Directory:")
            appendLine("========================")
            appendLine()
            appendLine("Config Directory: ${configDir.absolutePath}")
            appendLine("Directory Exists: ${configDir.exists()}")
            appendLine()
            
            if (configDir.exists() && configDir.isDirectory) {
                val files = configDir.listFiles()?.filter { it.isFile } ?: emptyList()
                appendLine("Configuration Files (${files.size}):")
                files.forEach { file ->
                    appendLine("  - ${file.name} (${file.length()} bytes)")
                }
            } else {
                appendLine("Configuration directory not found or not accessible.")
            }
        }
    }

    private fun buildFilesPromptsInfo(): String {
        return buildString {
            appendLine("Prompt Library Files:")
            appendLine("=====================")
            appendLine()
            
            // Check for prompt resources
            val resourcePaths = listOf(
                "/resources/prompts.yaml",
                "/prompts.yaml"
            )
            
            resourcePaths.forEach { path ->
                val resource = javaClass.getResourceAsStream(path)
                appendLine("Resource: $path")
                appendLine("  Available: ${resource != null}")
                resource?.close()
            }
            appendLine()
            
            // Check for external prompt files
            val promptFiles = listOf(
                File("prompts.yaml"),
                File("config/prompts.yaml"),
                File("prompts")
            )
            
            appendLine("External Prompt Files:")
            promptFiles.forEach { file ->
                appendLine("  ${file.path}: ${if (file.exists()) "EXISTS" else "not found"}")
            }
        }
    }

    private fun buildFilesYamlInfo(): String {
        val configDir = File("config")
        return buildString {
            appendLine("YAML Configuration Files:")
            appendLine("=========================")
            appendLine()
            
            if (configDir.exists() && configDir.isDirectory) {
                val yamlFiles = configDir.listFiles { file -> 
                    file.name.endsWith(".yaml") || file.name.endsWith(".yml")
                } ?: emptyArray()
                
                appendLine("YAML Files in config/ (${yamlFiles.size}):")
                yamlFiles.forEach { file ->
                    appendLine("  - ${file.name}")
                    appendLine("    Size: ${file.length()} bytes")
                    appendLine("    Last Modified: ${java.time.Instant.ofEpochMilli(file.lastModified())}")
                    appendLine()
                }
            } else {
                appendLine("Config directory not accessible.")
            }
            
            // Also check root directory for YAML files
            val rootDir = File(".")
            val rootYamlFiles = rootDir.listFiles { file ->
                (file.name.endsWith(".yaml") || file.name.endsWith(".yml")) && file.isFile
            } ?: emptyArray()
            
            if (rootYamlFiles.isNotEmpty()) {
                appendLine("YAML Files in root directory (${rootYamlFiles.size}):")
                rootYamlFiles.forEach { file ->
                    appendLine("  - ${file.name}")
                }
            }
        }
    }

    override suspend fun processUserInput() = TODO("Configuration view does not process user input")
}