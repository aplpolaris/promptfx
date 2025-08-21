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
package tri.promptfx.tools

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.TextPlugin
import tri.ai.pips.AiPipelineResult
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiTaskView
import tri.promptfx.PromptFxController
import tri.promptfx.PromptFxModels
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [GlobalConfigurationView]. */
class GlobalConfigurationPlugin : NavigableWorkspaceViewImpl<GlobalConfigurationView>("Settings", "Global Configuration", type = GlobalConfigurationView::class)

/** A view designed to show global configuration information for PromptFx. */
class GlobalConfigurationView : AiTaskView("Global Configuration", "Display global configuration settings and available models/plugins.", showInput = false) {

    init {
        hideParameters()
        outputPane.clear()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    init {
        output {
            vbox {
                vgrow = Priority.ALWAYS
                padding = insets(10)
                spacing = 15.0

                toolbar {
                    button("", FontAwesomeIconView(FontAwesomeIcon.REFRESH)) {
                        tooltip("Refresh configuration information.")
                        action { refresh() }
                    }
                }

                // Current Policy Section
                vbox {
                    spacing = 10.0
                    label("Current Policy") {
                        style {
                            fontSize = 16.px
                            fontWeight = javafx.scene.text.FontWeight.BOLD
                        }
                    }
                    label("Policy: ${PromptFxModels.policy}")
                    label("Show Banner: ${PromptFxModels.policy.isShowBanner}")
                    label("Show Usage: ${PromptFxModels.policy.isShowUsage}")
                    label("Show API Key Button: ${PromptFxModels.policy.isShowApiKeyButton}")
                }

                separator()

                // Current Default Models Section
                vbox {
                    spacing = 10.0
                    label("Default Models") {
                        style {
                            fontSize = 16.px
                            fontWeight = javafx.scene.text.FontWeight.BOLD
                        }
                    }
                    label("Text Completion: ${controller.completionEngine.value?.modelId ?: "None"}")
                    label("Chat: ${controller.chatService.value?.modelId ?: "None"}")
                    label("Embedding: ${controller.embeddingService.value?.modelId ?: "None"}")
                }

                separator()

                // Available Models Section
                scrollpane {
                    vgrow = Priority.ALWAYS
                    vbox {
                        spacing = 10.0
                        label("Available Models by Type") {
                            style {
                                fontSize = 16.px
                                fontWeight = javafx.scene.text.FontWeight.BOLD
                            }
                        }

                        // Text Completion Models
                        vbox {
                            spacing = 5.0
                            label("Text Completion Models:") {
                                style { fontWeight = javafx.scene.text.FontWeight.BOLD }
                            }
                            PromptFxModels.textCompletionModels().forEach { model ->
                                label("  • ${model.modelId}")
                            }
                        }

                        // Chat Models
                        vbox {
                            spacing = 5.0
                            label("Chat Models:") {
                                style { fontWeight = javafx.scene.text.FontWeight.BOLD }
                            }
                            PromptFxModels.chatModels().forEach { model ->
                                label("  • ${model.modelId}")
                            }
                        }

                        // Embedding Models
                        vbox {
                            spacing = 5.0
                            label("Embedding Models:") {
                                style { fontWeight = javafx.scene.text.FontWeight.BOLD }
                            }
                            PromptFxModels.embeddingModels().forEach { model ->
                                label("  • ${model.modelId}")
                            }
                        }

                        // Multimodal Models
                        if (PromptFxModels.multimodalModels().isNotEmpty()) {
                            vbox {
                                spacing = 5.0
                                label("Multimodal Models:") {
                                    style { fontWeight = javafx.scene.text.FontWeight.BOLD }
                                }
                                PromptFxModels.multimodalModels().forEach { model ->
                                    label("  • ${model.modelId}")
                                }
                            }
                        }

                        // Vision Language Models
                        if (PromptFxModels.visionLanguageModels().isNotEmpty()) {
                            vbox {
                                spacing = 5.0
                                label("Vision Language Models:") {
                                    style { fontWeight = javafx.scene.text.FontWeight.BOLD }
                                }
                                PromptFxModels.visionLanguageModels().forEach { model ->
                                    label("  • ${model.modelId}")
                                }
                            }
                        }

                        // Image Models
                        if (PromptFxModels.imageModels().isNotEmpty()) {
                            vbox {
                                spacing = 5.0
                                label("Image Generation Models:") {
                                    style { fontWeight = javafx.scene.text.FontWeight.BOLD }
                                }
                                PromptFxModels.imageModels().forEach { model ->
                                    label("  • ${model.modelId}")
                                }
                            }
                        }

                        separator()

                        // Plugins Section
                        vbox {
                            spacing = 10.0
                            label("Available Plugins") {
                                style {
                                    fontSize = 16.px
                                    fontWeight = javafx.scene.text.FontWeight.BOLD
                                }
                            }
                            TextPlugin.orderedPlugins.forEach { plugin ->
                                vbox {
                                    spacing = 3.0
                                    label("Plugin: ${plugin.modelSource()}") {
                                        style { fontWeight = javafx.scene.text.FontWeight.BOLD }
                                    }
                                    label("  Models: ${plugin.modelInfo().size}")
                                    label("  Text Completion: ${plugin.textCompletionModels().size}")
                                    label("  Chat: ${plugin.chatModels().size}")
                                    label("  Embedding: ${plugin.embeddingModels().size}")
                                    label("  Multimodal: ${plugin.multimodalModels().size}")
                                    label("  Vision Language: ${plugin.visionLanguageModels().size}")
                                    label("  Image Generator: ${plugin.imageGeneratorModels().size}")
                                }
                            }
                        }

                        separator()

                        // Prompt Configuration
                        vbox {
                            spacing = 10.0
                            label("Prompt Configuration") {
                                style {
                                    fontSize = 16.px
                                    fontWeight = javafx.scene.text.FontWeight.BOLD
                                }
                            }
                            label("Built-in Prompts: ${AiPromptLibrary.INSTANCE.prompts.size}")
                            label("Runtime Prompts: ${AiPromptLibrary.RUNTIME_INSTANCE.prompts.size}")
                        }

                        separator()

                        // Usage Statistics
                        vbox {
                            spacing = 10.0
                            label("Usage Statistics") {
                                style {
                                    fontSize = 16.px
                                    fontWeight = javafx.scene.text.FontWeight.BOLD
                                }
                            }
                            label("Tokens Used: ${controller.tokensUsed.value}")
                            label("Audio Used (seconds): ${controller.audioUsed.value}")
                            label("Images Used: ${controller.imagesUsed.value}")
                        }
                    }
                }
            }
        }
    }

    private fun refresh() {
        controller.updateUsage()
        // This view is read-only, just update the usage stats
    }

    override suspend fun processUserInput(): AiPipelineResult<*> {
        // This view is read-only and doesn't process user input
        return AiPipelineResult.todo<String>()
    }
}