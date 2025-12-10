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
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.util.Callback
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.gemini.GeminiAiPlugin
import tri.ai.openai.OpenAiPlugin
import tri.ai.openai.api.OpenAiApiPlugin
import tri.promptfx.*
import tri.promptfx.PromptFxConfig.Companion.DIR_KEYS
import tri.promptfx.api.ModelsView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import java.io.File

/** Plugin for the [PromptFxSettingsView]. */
class PromptFxSettingsPlugin : NavigableWorkspaceViewImpl<PromptFxSettingsView>("Settings", "PromptFx Settings", type = PromptFxSettingsView::class)

/** Configuration view showing application configuration and runtime information. */
class PromptFxSettingsView : AiTaskView("PromptFx Settings", "View and manage application configuration settings.") {

    private var category = SimpleObjectProperty<ConfigCategory>(ConfigCategory.ROOT)
    private var detailPane: VBox by singleAssign()
    
    init {
        hideParameters()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    init {
        inputPane.clear()
        outputPane.clear()
        lateinit var treeSelectionModel: MultipleSelectionModel<TreeItem<ConfigCategory>>
        input {
            // Left panel - Tree navigation
            vbox {
                vgrow = Priority.ALWAYS

                treeview<ConfigCategory> {
                    vgrow = Priority.ALWAYS
                    prefWidth = 250.0

                    root = TreeItem(ConfigCategory.ROOT)
                    root.isExpanded = true

                    // Add category items
                    root.children.addAll(
                        (ConfigCategory.entries.toTypedArray().toSet() - ConfigCategory.ROOT)
                            .map { TreeItem(it) }
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
                                style = "-fx-font-size:14"
                            }
                        }
                    }
                    treeSelectionModel = selectionModel
                }
            }
        }
        output {
            scrollpane {
                vgrow = Priority.ALWAYS
                hgrow = Priority.ALWAYS

                vbox(10) {
                    paddingAll = 20.0
                    label(category.stringBinding { it!!.title }) {
                        style {
                            fontSize = 18.px
                            fontWeight = FontWeight.BOLD
                        }
                    }
                    label(category.stringBinding { it!!.description }) {
                        style { fontSize = 14.px }
                    }
                    separator()
                    detailPane = vbox(10) { }
                }
            }
        }

        treeSelectionModel.selectedItemProperty().addListener { _, _, newValue ->
            category.set(newValue.value!!)
            updateCategoryDetails()
        }
        treeSelectionModel.select(1)
    }
    
    private fun updateCategoryDetails() {
        detailPane.clear()
        when (category.value) {
            ConfigCategory.APIS -> showApis()
            ConfigCategory.RUNTIME -> showRuntimeDetails()
            ConfigCategory.MCP_SERVERS -> showMcpServersDetails()
            ConfigCategory.STARSHIP -> showStarshipConfigDetails()
            ConfigCategory.CONFIG_FILES -> showConfigFilesDetails()
            ConfigCategory.VIEWS -> showViewsDetails()
            ConfigCategory.SESSION -> showSessionConfigDetails()
            ConfigCategory.SYSTEM -> showSystemConfigDetails()
            else -> { }
        }
    }

    private fun showApis() {
        with(detailPane) {
            // Current Model Policy
            vbox(5) {
                label("Current Model Policy:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Policy Type: ${PromptFxModels.policy.javaClass.simpleName}")
                label("Show Banner: ${PromptFxModels.policy.isShowBanner}")
                label("Default Completion Model: ${PromptFxModels.policy.textCompletionModelDefault().modelId}")
                label("Default Chat Model: ${PromptFxModels.policy.chatModelDefault()!!.modelId}")
                label("Default Embedding Model: ${PromptFxModels.policy.embeddingModelDefault().modelId}")
            }

            separator()

            // All Discovered Plugins
            val allPlugins = TextPlugin.orderedPlugins
            val policyPlugins = PromptFxModels.policy.supportedPlugins()
            vbox(5) {
                label("Current Plugins:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                allPlugins.forEach { plugin ->
                    val status = if (plugin in policyPlugins) "(Active)" else "(Inactive)"
                    label("• ${plugin.javaClass.simpleName} $status") {
                        runAsync {
                            try {
                                runBlocking {
                                    withTimeout(5000) { // 5 second timeout per plugin
                                        plugin.modelInfo().size
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                -1 // Indicate timeout
                            } catch (e: Exception) {
                                -2 // Indicate error
                            }
                        } ui { modelCount ->
                            text += when (modelCount) {
                                -1 -> " (timeout)"
                                -2 -> " (error)"
                                else -> " ($modelCount models)"
                            }
                        }
                    }
                }
            }

            separator()

            // API client configurations
            allPlugins.forEach {
                label("${it.javaClass.simpleName} API Client:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                vbox(5) {
                    label("Model Source: ${it.modelSource()}")
                    when (it) {
                        is OpenAiPlugin -> {
                            label("API Key: ${it.client.settings.apiKey?.let { "Configured" } ?: "Not Configured"}")
                            label("Base URL: ${it.client.settings.baseUrl}")
                            label("Log Level: ${it.client.settings.logLevel}")
                        }

                        is GeminiAiPlugin -> {
                            label("API Key: ${it.client.settings.apiKey?.let { "Configured" } ?: "Not Configured"}")
                            label("Base URL: ${it.client.settings.baseUrl}")
                            label("Timeout: ${it.client.settings.timeoutSeconds}sec")

                        }

                        is OpenAiApiPlugin -> {
                            it.config.endpoints.forEach {
                                label("• ${it.source}") {
                                    style { fontWeight = FontWeight.NORMAL }
                                }
                                label("  API Key: ${it.settings.apiKey?.let { "Configured" } ?: "Not Configured"}")
                                label("  Base URL: ${it.settings.baseUrl}")
                                label("  Log Level: ${it.settings.logLevel}")
                                label("  Timeout: ${it.settings.timeoutSeconds}sec")
                            }
                        }

                        else -> {
                            label("No specific API client configuration available for ${it.javaClass.simpleName}.")
                        }
                    }
                }
            }

            separator()

            // Available Models
            vbox(5) {
                label("Available Models:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                hyperlink("See Models View") {
                    action { workspace.dock(find<ModelsView>()) }
                }
            }
        }
    }
    
    private fun showRuntimeDetails() {
        with(detailPane) {

            // Current Policy
            vbox(5) {
                label("Current Policy:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Policy: ${PromptFxModels.policy.javaClass.simpleName}")
                if (PromptFxModels.policy.isShowBanner) {
                    label("Banner: ${PromptFxModels.policy.bar.text}") {
                        style {
                            backgroundColor += PromptFxModels.policy.bar.bgColor
                            textFill = PromptFxModels.policy.bar.fgColor
                            padding = box(2.px, 4.px)
                            fontWeight = FontWeight.BOLD
                        }
                    }
                }
            }

            separator()
            
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
                label(controller.embeddingStrategy.stringBinding { "Provider: ${it?.model?.javaClass?.simpleName ?: "Unknown"}" })
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
        }
    }

    private fun showMcpServersDetails() {
        with(detailPane) {
            val mcpController = find<PromptFxMcpController>()
            val registry = mcpController.mcpServerRegistry
            
            // Registry Information
            vbox(5) {
                label("MCP Server Registry:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                label("Total Configured Servers: ${registry.listServerNames().size}")
            }
            
            separator()
            
            // Configuration Files
            vbox(5) {
                label("Configuration Files:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val configFiles = listOf(
                    File("mcp-servers.yaml"),
                    File("config/mcp-servers.yaml")
                )
                configFiles.forEach { file ->
                    label("• ${file.path}: ${if (file.exists()) "Found (${file.length()} bytes)" else "Not found"}")
                }
            }
            
            separator()
            
            // List all configured servers
            val configs = registry.getConfigs()
            if (configs.isEmpty()) {
                vbox(5) {
                    label("No MCP servers configured.") {
                        style { fontStyle = FontPosture.ITALIC }
                    }
                }
            } else {
                vbox(5) {
                    label("Configured Servers (${configs.size}):") {
                        style { fontWeight = FontWeight.BOLD }
                    }
                    
                    configs.forEach { (name, config) ->
                        vbox(5) {
                            label("• $name") {
                                style { fontWeight = FontWeight.BOLD }
                            }
                            label("  Type: ${config.javaClass.simpleName.replace("ServerConfig", "")}")
                            config.description?.let { desc ->
                                label("  Description: $desc")
                            }
                            
                            // Show config-specific details
                            when (config) {
                                is tri.ai.mcp.StdioServerConfig -> {
                                    label("  Command: ${config.command}")
                                    if (config.args.isNotEmpty()) {
                                        label("  Arguments: ${config.args.joinToString(" ")}")
                                    }
                                    if (config.env.isNotEmpty()) {
                                        label("  Environment Variables: ${config.env.size}")
                                    }
                                }
                                is tri.ai.mcp.HttpServerConfig -> {
                                    label("  URL: ${config.url}")
                                }
                                is tri.ai.mcp.EmbeddedServerConfig -> {
                                    config.promptLibraryPath?.let { path ->
                                        label("  Prompt Library: $path")
                                    }
                                }
                                is tri.ai.mcp.TestServerConfig -> {
                                    label("  Include Default Prompts: ${config.includeDefaultPrompts}")
                                    label("  Include Default Tools: ${config.includeDefaultTools}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showStarshipConfigDetails() {
        with(detailPane) {

            // TODO - update this with starship configuration details

        }
    }

    private fun showConfigFilesDetails() {
        with(detailPane) {

            // Config Directory
            vbox(5) {
                label("Config Directory:") {
                    style { fontWeight = FontWeight.BOLD }
                }
                val configDir = File("config")
                label("Path: ${configDir.absolutePath}")

                if (configDir.exists() && configDir.isDirectory) {
                    label("Contents:")
                    configDir.listFiles()?.forEach { file ->
                        label("  • ${file.name} (${if (file.isDirectory) "directory" else "file, ${file.length()} bytes"})")
                    }
                } else {
                    label("Does not exist or not a directory.") {
                        style { fontStyle = FontPosture.ITALIC }
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
        }
    }
    
    private fun showViewsDetails() {
        with(detailPane) {
            
            // Add "Create New Custom View" button
            hbox(10) {
                paddingBottom = 15.0
                button("Create New Custom View...") {
                    graphic = FontAwesomeIcon.PLUS.graphic
                    action {
                        val dialog = find<NewViewDialog>()
                        dialog.openModal()
                        // Refresh the view details after dialog closes
                        runLater {
                            updateCategoryDetails()
                        }
                    }
                }
            }

            // show views registered by source
            val viewsBySource = RuntimePromptViewConfigs.viewConfigs.groupBy { it.source }
            viewsBySource.forEach { (source, list) ->
                vbox(5) {
                    label("$source Views (${list.size}):") {
                        style { fontWeight = FontWeight.BOLD }
                    }
                    list.groupBy { it.viewGroup }.forEach { (group, views) ->
                        label("  $group:")
                        views.forEach { view ->
                            val isOverwritten = RuntimePromptViewConfigs.isOverwritten(view)
                            if (isOverwritten) {
                                label("    • ${view.viewId} (overwritten)") {
                                    style { fontStyle = FontPosture.ITALIC }
                                }
                            } else {
                                label("    • ${view.viewId}")
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        label("  No views found")
                    }
                }
            }
        }
    }
    
    private fun showSessionConfigDetails() {
        with(detailPane) {
            
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
                DIR_KEYS.forEach { key ->
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
                    label("  • ${file.path} (${if (file.exists()) "${file.length()} bytes" else "missing"})")
                }
                
                val textClusterFiles = promptFxConfig.textClusterFiles()
                label("Text Cluster Files (${textClusterFiles.size}):")
                textClusterFiles.forEach { file ->
                    label("  • ${file.path} (${if (file.exists()) "${file.length()} bytes" else "missing"})")
                }
            }
        }
    }

    private fun showSystemConfigDetails() {
        with (detailPane) {
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

    override suspend fun processUserInput() = TODO("Configuration view does not process user input")
}

/** Categories for configuration tree navigation. */
enum class ConfigCategory(val displayName: String, val title: String, val description: String, val icon: FontAwesomeIcon) {
    ROOT("Configuration", "PromptFx Settings", "Select a specific category to view details.", FontAwesomeIcon.COG),
    APIS("APIs", "APIs", "External APIs (model providers, etc.).", FontAwesomeIcon.CLOUD),
    RUNTIME("Model Runtime", "Model Runtime", "Current policy, models, and usage statistics.", FontAwesomeIcon.GEARS),
    MCP_SERVERS("MCP Servers", "MCP Servers", "Model Context Protocol server configurations.", FontAwesomeIcon.SERVER),
    CONFIG_FILES("Config Files", "Configuration Files", "Configuration files discovered by PromptFx.", FontAwesomeIcon.FILE_CODE_ALT),
    VIEWS("Views", "View Configuration", "Current and discovered view configurations.", FontAwesomeIcon.SITEMAP),
    STARSHIP("Starship Mode", "Starship Demo Mode Config", "View configuration for \"Starship\" demo mode.", FontAwesomeIcon.ROCKET),
    SESSION("Session", "Session Config", "Information being saved/restored between sessions.", FontAwesomeIcon.WRENCH),
    SYSTEM("System", "System Information", "System information and application details.", FontAwesomeIcon.INFO_CIRCLE)
}