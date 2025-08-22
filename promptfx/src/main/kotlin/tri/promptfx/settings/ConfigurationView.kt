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
import javafx.scene.control.*
import javafx.scene.layout.Priority
import tornadofx.*
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxModels
import tri.promptfx.PromptFxWorkspace
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.graphic

/** Plugin for the [ConfigurationView]. */
class ConfigurationPlugin : NavigableWorkspaceViewImpl<ConfigurationView>("Settings", "Configuration", WorkspaceViewAffordance.NONE, ConfigurationView::class)

/** Configuration section types. */
enum class ConfigSection(val displayName: String) {
    RUNTIME("Runtime"),
    APIS_MODELS("APIs/Models"),
    VIEWS("Views"),
    CONFIG_FILES("Configuration Files")
}

/** View for application configuration settings. */
class ConfigurationView : View("Configuration") {

    private val controller: PromptFxController by inject()
    private val promptFxWorkspace: PromptFxWorkspace by inject()
    
    private val selectedSection = SimpleObjectProperty<ConfigSection>(ConfigSection.RUNTIME)
    
    override val root = borderpane {
        left = vbox {
            label("Configuration") {
                style {
                    fontSize = 16.px
                    fontWeight = javafx.scene.text.FontWeight.BOLD
                }
                padding = insets(10.0)
            }
            
            listview<ConfigSection> {
                items.setAll(ConfigSection.values().toList())
                
                cellFormat {
                    text = it.displayName
                    graphic = when (it) {
                        ConfigSection.RUNTIME -> FontAwesomeIcon.GEAR.graphic
                        ConfigSection.APIS_MODELS -> FontAwesomeIcon.CLOUD.graphic  
                        ConfigSection.VIEWS -> FontAwesomeIcon.SITEMAP.graphic
                        ConfigSection.CONFIG_FILES -> FontAwesomeIcon.FILE_TEXT.graphic
                    }
                }
                
                selectedSection.bind(selectionModel.selectedItemProperty())
                selectionModel.select(0)
                
                prefWidth = 200.0
                vgrow = Priority.ALWAYS
            }
        }
        
        center = scrollpane {
            isFitToWidth = true
            
            content = vbox {
                selectedSection.onChange { newSection ->
                    clear()
                    add(when (newSection) {
                        ConfigSection.RUNTIME -> createRuntimeSection()
                        ConfigSection.APIS_MODELS -> createApisModelsSection()
                        ConfigSection.VIEWS -> createViewsSection()  
                        ConfigSection.CONFIG_FILES -> createConfigFilesSection()
                        null -> label("Select a configuration section")
                    })
                }
                // Initialize with first section
                add(createRuntimeSection())
            }
        }
    }
    
    /** Create the Runtime configuration section. */
    private fun createRuntimeSection() = vbox {
        padding = insets(20.0)
        spacing = 15.0
        
        label("Runtime Configuration") {
            style {
                fontSize = 18.px
                fontWeight = javafx.scene.text.FontWeight.BOLD
            }
        }
        
        separator()
        
        // Global objects managed by PromptFxController
        titledpane("Global Objects", collapsible = false) {
            form {
                fieldset("Current Models") {
                    field("Completion Engine") {
                        label(controller.completionEngine.stringBinding { it?.modelId ?: "None" })
                    }
                    field("Chat Service") {
                        label(controller.chatService.stringBinding { it?.modelId ?: "None" })
                    }
                    field("Embedding Service") {
                        label(controller.embeddingService.stringBinding { it?.modelId ?: "None" })
                    }
                }
                
                fieldset("Usage Statistics") {
                    field("Tokens Used") {
                        label(controller.tokensUsed.stringBinding { "$it" })
                    }
                    field("Audio Used") {
                        label(controller.audioUsed.stringBinding { "$it seconds" })
                    }
                    field("Images Used") {
                        label(controller.imagesUsed.stringBinding { "$it" })
                    }
                }
                
                fieldset("Current Policy") {
                    field("Policy Type") {
                        label(PromptFxModels.policy.toString())
                    }
                    field("Show Banner") {
                        checkbox("", property = booleanProperty(PromptFxModels.policy.isShowBanner)) {
                            isDisable = true
                        }
                    }
                    field("Show Usage") {
                        checkbox("", property = booleanProperty(PromptFxModels.policy.isShowUsage)) {
                            isDisable = true
                        }
                    }
                }
            }
        }
    }
    
    /** Create the APIs/Models configuration section. */
    private fun createApisModelsSection() = vbox {
        padding = insets(20.0)
        spacing = 15.0
        
        label("APIs/Models Configuration") {
            style {
                fontSize = 18.px
                fontWeight = javafx.scene.text.FontWeight.BOLD
            }
        }
        
        separator()
        
        titledpane("Current Model Policy", collapsible = false) {
            form {
                fieldset("Policy Information") {
                    field("Type") {
                        label(PromptFxModels.policy.toString())
                    }
                    field("Banner Color") {
                        label("${PromptFxModels.policy.bar.bgColor}")
                    }
                    field("Show Banner") {
                        checkbox("", property = booleanProperty(PromptFxModels.policy.isShowBanner)) {
                            isDisable = true
                        }
                    }
                }
            }
        }
        
        titledpane("Available Model Plugins", collapsible = false) {
            listview<tri.ai.core.TextPlugin> {
                items.setAll(tri.ai.core.TextPlugin.orderedPlugins)
                
                cellFormat {
                    text = "${it.modelSource()} - ${it.javaClass.simpleName} (${it.modelInfo().size} models)"
                }
                
                prefHeight = 200.0
            }
        }
    }
    
    /** Create the Views configuration section. */
    private fun createViewsSection() = vbox {
        padding = insets(20.0)
        spacing = 15.0
        
        label("Views Configuration") {
            style {
                fontSize = 18.px
                fontWeight = javafx.scene.text.FontWeight.BOLD
            }
        }
        
        separator()
        
        titledpane("Registered Views", collapsible = false) {
            treeview<String> {
                root = TreeItem("PromptFx Views")
                isShowRoot = true
                
                // Add categories and their views
                promptFxWorkspace.views.forEach { (category, views) ->
                    val categoryItem = TreeItem(category)
                    root.children.add(categoryItem)
                    
                    views.forEach { (viewName, _) ->
                        categoryItem.children.add(TreeItem(viewName))
                    }
                }
                
                cellFormat {
                    text = item
                    graphic = if (treeItem.isLeaf && treeItem.parent != root) {
                        FontAwesomeIcon.FILE.graphic
                    } else {
                        FontAwesomeIcon.FOLDER.graphic
                    }
                }
                
                root.isExpanded = true
                prefHeight = 300.0
            }
        }
    }
    
    /** Create the Configuration Files section. */
    private fun createConfigFilesSection() = vbox {
        padding = insets(20.0)
        spacing = 15.0
        
        label("Configuration Files") {
            style {
                fontSize = 18.px
                fontWeight = javafx.scene.text.FontWeight.BOLD
            }
        }
        
        separator()
        
        titledpane("Configuration Directories", collapsible = false) {
            form {
                fieldset("Standard Locations") {
                    field("Config Directory") {
                        label("config/")
                    }
                    field("Resources Directory") {
                        label("resources/")
                    }
                }
                
                fieldset("Configuration Files Found") {
                    field("Views Configuration") {
                        vbox {
                            label("views.yaml (if exists)")
                            label("config/views.yaml (if exists)")
                        }
                    }
                    field("Modes Configuration") {
                        vbox {
                            label("modes.yaml (if exists)")
                            label("config/modes.yaml (if exists)")
                        }
                    }
                    field("Prompt Library") {
                        vbox {
                            label("prompts.yaml files")
                            label("Various prompt library configurations")
                        }
                    }
                }
            }
        }
        
        titledpane("Runtime Configurations", collapsible = false) {
            textarea {
                text = "Runtime configuration summary:\n\n" +
                        "Categories loaded: ${tri.promptfx.RuntimePromptViewConfigs.categories().joinToString(", ")}\n" +
                        "Total configurations: ${tri.promptfx.RuntimePromptViewConfigs.categories().sumOf { tri.promptfx.RuntimePromptViewConfigs.configs(it).size }}"
                isEditable = false
                prefRowCount = 5
            }
        }
    }
}