/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.Modality
import javafx.stage.StageStyle
import tornadofx.*
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxConfig.Companion.DIR_KEY_TXT
import tri.promptfx.PromptFxConfig.Companion.FF_ALL
import tri.promptfx.PromptFxConfig.Companion.FF_TXT
import tri.promptfx.ui.PromptTraceDetails
import java.util.*
import java.util.zip.Deflater
import java.util.zip.Deflater.DEFLATED


/**
 * Text area for displaying a prompt result or other output. Adjusts font size, adds ability to copy/save output to a file.
 */
class PromptResultArea : Fragment("Prompt Result Area") {

    private val text = SimpleStringProperty("")
    val trace = SimpleObjectProperty<AiPromptTrace>(null)

    private val containsCode = text.booleanBinding { it != null && it.lines().count { it.startsWith("```") } >= 2 }
    private val containsPlantUml = text.booleanBinding { it != null && (
            it.contains("@startuml") && it.contains("@enduml") ||
            it.contains("@startmindmap") && it.contains("@endmindmap")
        )
    }

    fun setFinalResult(finalResult: AiPromptTrace) {
        text.set(finalResult.outputInfo.output ?: "(No result)")
        trace.set(finalResult as? AiPromptTrace)
    }

    override val root = textarea(text) {
        promptText = "Prompt output will be shown here"
        isEditable = false
        isWrapText = true
        font = Font("Segoe UI Emoji", 18.0)
        vgrow = Priority.ALWAYS

        // add context menu option to save result to a file
        contextmenu {
            item("Details...") {
                enableWhen { trace.isNotNull }
                action {
                    find<PromptTraceDetails>().apply {
                        setTrace(trace.get())
                        openModal()
                    }
                }
            }
            item("Try in template view") {
                enableWhen(trace.booleanBinding { it != null && it.promptInfo.prompt.isNotBlank() })
                action {
                    (workspace as PromptFxWorkspace).launchTemplateView(trace.value)
                }
            }
            separator()
            item("Select all") {
                action { selectAll() }
            }
            item("Copy") {
                action { copy() }
            }
            item("Save to file...") {
                action {
                    promptFxFileChooser(
                        dirKey = DIR_KEY_TXT,
                        title = "Save to File",
                        filters = arrayOf(FF_TXT, FF_ALL),
                        mode = FileChooserMode.Save
                    ) {
                        it.firstOrNull()?.writeText(selectedText.ifBlank { this@textarea.text })
                    }
                }
            }
            separator()
            item("Copy code") {
                enableWhen(containsCode)
                action { copyCode() }
            }
            item("Browse to PlantUML diagram") {
                enableWhen(containsPlantUml)
                action { browseToPlantUml() }
            }
            item("Show PlantUML diagram") {
                enableWhen(containsPlantUml)
                action { showPlantUmlPopup() }
            }
        }
    }

    private fun copyCode() {
        val code = text.value.substringAfter("```").substringAfter("\n").substringBefore("```").trim()
        clipboard.putString(code)
    }

    private fun plantUmlText() = if ("@startuml" in text.value) {
        "@startuml\n" + text.value.substringAfter("@startuml").substringBefore("@enduml").trim() + "\n@enduml"
    } else {
        "@startmindmap\n" + text.value.substringAfter("@startmindmap").substringBefore("@endmindmap").trim() + "\n@endmindmap"
    }

    private fun browseToPlantUml() {
        val url = plantUmlUrlText(plantUmlText())
        println(url)
        hostServices.showDocument(url)
    }

    private fun showPlantUmlPopup() {
        val url = plantUmlUrlText(plantUmlText())
        val image = Image(url)
        if (!image.isError && !image.isBackgroundLoading && image.width > 0.0 && image.height > 0.0)
            showImageDialog(image)
        else
            error("Error loading PlantUML diagram.")
    }

    // TODO - COPIED FROM ImagesView - remove if possible
    private fun showImageDialog(image: Image) {
        val d = dialog(
            modality = Modality.APPLICATION_MODAL,
            stageStyle = StageStyle.UNDECORATED,
            owner = primaryStage
        ) {
            imageview(image) {
                style = "-fx-border-color: black; -fx-border-width: 1px;"
                onLeftClick { close() }
            }
            form.padding = insets(0)
            padding = insets(0)
        }
        // center dialog on window (dialog method doesn't do this because it adds content after centering on owner)
        d?.owner?.let {
            d.x = it.x + (it.width / 2) - (d.scene.width / 2)
            d.y = it.y + (it.height / 2) - (d.scene.height / 2)
        }
    }

    companion object {
        //region PlantUML specific encoding of URLs
        private fun plantUmlUrlText(plantUml: String): String {
            val compressor = Deflater(DEFLATED, false)
            compressor.setInput(plantUml.toByteArray(Charsets.UTF_8))
            compressor.finish()
            val compressedData = ByteArray(plantUml.length)
            val compressedSize = compressor.deflate(compressedData)
            compressor.end()
            val encodedUml = Base64.getEncoder().withoutPadding()
                .encodeToString(compressedData.copyOf(compressedSize))
                .convertToPlantUmlBase()
            return "http://www.plantuml.com/plantuml/png/~1${encodedUml}"
        }

        private const val FROM_ARR = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        private const val TO_ARR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_="

        private fun String.convertToPlantUmlBase() = try {
            map { TO_ARR[FROM_ARR.indexOf(it)] }.joinToString("")
        } catch (x: ArrayIndexOutOfBoundsException) {
            println("Error converting to PlantUML base: $this")
        }
        //endregion
    }

}
