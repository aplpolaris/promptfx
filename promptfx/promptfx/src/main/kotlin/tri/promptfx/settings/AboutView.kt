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
package tri.promptfx.settings

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import tri.promptfx.AiTaskView
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.graphic
import java.io.File
import java.util.Properties

/** Plugin for the [AboutView]. */
class AboutPlugin : NavigableWorkspaceViewImpl<AboutView>("Settings", "About", type = AboutView::class)

/** About view showing application information. */
class AboutView : AiTaskView("About PromptFx", "Information about the PromptFx application.", showInput = false) {

    init {
        hideParameters()
        runButton.isVisible = false
        runButton.isManaged = false
    }

    init {
        outputPane.clear()
        output {
            vgrow = Priority.ALWAYS

            vbox(20) {
                alignment = Pos.CENTER
                paddingAll = 40.0

                vbox(10) {
                    alignment = Pos.CENTER

                    label {
                        graphic = FontAwesomeIcon.ROCKET.graphic.apply {
                            fill = Color.CORNFLOWERBLUE
                            glyphSize = 64
                        }
                    }

                    label("PromptFx") {
                        style {
                            fontSize = 36.px
                            fontWeight = FontWeight.BOLD
                            textFill = Color.DARKBLUE
                        }
                    }

                    label("Version ${getApplicationVersion()}") {
                        style {
                            fontSize = 18.px
                            fontWeight = FontWeight.NORMAL
                            textFill = Color.GRAY
                        }
                    }
                }

                vbox(15) {
                    alignment = Pos.CENTER
                    maxWidth = 600.0

                    label("GAI Prompt Engineering and Demonstration Tool") {
                        style {
                            fontSize = 20.px
                            fontWeight = FontWeight.BOLD
                        }
                        isWrapText = true
                    }

                    text {
                        text = "PromptFx provides a prototype Kotlin module for AI prompt chaining (promptkt) " +
                                "and an associated JavaFx demonstration UI. It is designed primarily for " +
                                "demonstration and exploration purposes with the OpenAI API, Google Gemini, " +
                                "and other compatible LLM APIs."
                        wrappingWidth = 600.0
                        style {
                            fontSize = 14.px
                        }
                    }
                }

                vbox(10) {
                    alignment = Pos.CENTER_LEFT
                    maxWidth = 600.0

                    label("Key Features:") {
                        style {
                            fontSize = 16.px
                            fontWeight = FontWeight.BOLD
                        }
                    }

                    vbox(5) {
                        paddingLeft = 20.0

                        label("• API testing and model browsing")
                        label("• Prompt engineering and scripting tools")
                        label("• Document Q&A and text processing")
                        label("• Natural language processing tasks")
                        label("• Audio processing (speech-to-text, text-to-speech)")
                        label("• Image generation and vision tasks")
                        label("• Workflow automation and tool chaining")
                    }
                }

                // Developer information
                vbox(10) {
                    alignment = Pos.CENTER

                    label("Developed by") {
                        style {
                            fontSize = 14.px
                            fontWeight = FontWeight.BOLD
                        }
                    }

                    label("Johns Hopkins University Applied Physics Laboratory") {
                        style {
                            fontSize = 14.px
                        }
                    }

                    label("Copyright © 2023-2025") {
                        style {
                            fontSize = 12.px
                            textFill = Color.GRAY
                        }
                    }
                }

                // Links
                hbox(20) {
                    alignment = Pos.CENTER

                    hyperlink("GitHub Repository") {
                        graphic = FontAwesomeIcon.GITHUB.graphic
                        action {
                            hostServices.showDocument("https://github.com/aplpolaris/promptfx")
                        }
                    }

                    hyperlink("Wiki Documentation") {
                        graphic = FontAwesomeIcon.BOOK.graphic
                        action {
                            hostServices.showDocument("https://github.com/aplpolaris/promptfx/wiki")
                        }
                    }

                    hyperlink("License") {
                        graphic = FontAwesomeIcon.LEGAL.graphic
                        action {
                            hostServices.showDocument("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                }
            }
        }
    }

    override suspend fun processUserInput() = TODO("About view does not process user input")

    companion object {
        /**
         * Gets the application version dynamically from the package manifest or fallback sources.
         * This allows the version to be automatically updated without manual code changes.
         *
         * The method tries multiple approaches in order:
         * 1. Package implementation version (available in packaged JARs)
         * 2. Version from resources/version.properties (custom approach)
         * 3. Version from Maven-generated pom.properties (standard Maven approach)
         * 4. Fallback to hardcoded version if none of the above work
         *
         * @return The application version string, e.g., "0.11.2-SNAPSHOT"
         */
        fun getApplicationVersion(): String {
            // Try to get version from package implementation version (works in packaged JAR)
            AboutView::class.java.`package`?.implementationVersion?.let { return it }

            // Try to read from Maven filtered resources (another common approach)
            try {
                AboutView::class.java.getResourceAsStream("/META-INF/maven/com.googlecode.blaisemath/promptfx/pom.properties")?.use { stream ->
                    Properties().apply {
                        load(stream)
                        getProperty("version")?.let { return it }
                    }
                }
            } catch (e: Exception) {
                // Ignore, continue to fallback
            }

            // Try to read from pom xml file (development)
            val pom = File("pom.xml")
            if (pom.exists()) {
                pom.readText().lines().forEach { line ->
                    if (line.contains("<version>")) {
                        val version = line.substringAfter("<version>").substringBefore("</version>").trim()
                        if (version.isNotBlank())
                            return version
                    }
                }
            }

            // Ultimate fallback version
            return "dev SNAPSHOT"
        }
    }

}
