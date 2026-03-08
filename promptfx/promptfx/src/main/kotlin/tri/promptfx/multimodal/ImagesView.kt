/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.promptfx.multimodal

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.control.Slider
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import tornadofx.action
import tornadofx.button
import tornadofx.clear
import tornadofx.combobox
import tornadofx.contextmenu
import tornadofx.datagrid
import tornadofx.enableWhen
import tornadofx.error
import tornadofx.field
import tornadofx.imageview
import tornadofx.information
import tornadofx.integerBinding
import tornadofx.item
import tornadofx.label
import tornadofx.observableListOf
import tornadofx.onChange
import tornadofx.putString
import tornadofx.runLater
import tornadofx.separator
import tornadofx.slider
import tornadofx.text
import tornadofx.toolbar
import tornadofx.tooltip
import tornadofx.vbox
import tornadofx.vgrow
import tri.ai.core.ImageGenerationParams
import tri.ai.core.MultimodalChatMessage
import tri.ai.pips.aitask
import tri.ai.prompt.PromptTemplate
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiImageTrace
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.PromptInfo
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxModels
import tri.promptfx.promptFxDirectoryChooser
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.base64ToImage
import tri.util.ui.copyToClipboard
import tri.util.ui.saveToFile
import tri.util.ui.showImageDialog
import tri.util.ui.writeImageToFile

/** Plugin for the [ImagesView]. */
class ImagesApiPlugin : NavigableWorkspaceViewImpl<ImagesView>("Multimodal", "Text-to-Image", WorkspaceViewAffordance.Companion.INPUT_ONLY, ImagesView::class)

/** View for image generation, supporting multiple providers (e.g. OpenAI DALL-E, Gemini). */
class ImagesView : AiPlanTaskView("Images", "Enter image prompt") {

    /** User input */
    private val input = SimpleStringProperty("")
    /** Image results */
    private val images = observableListOf<AiImageTrace>()

    /** Available image model IDs from the active policy. */
    private val imageModelIds = observableListOf<String>().apply {
        setAll(PromptFxModels.imageModels().map { it.modelId }.distinct().ifEmpty { listOf(DALLE2_ID) })
    }

    /** Model */
    private val model = SimpleStringProperty(PromptFxModels.imageModelDefault()?.modelId ?: DALLE2_ID).apply {
        onChange {
            imageSizes.setAll(MODEL_INFO[it]?.sizes ?: listOf("1024x1024"))
            if (imageSize.value !in imageSizes)
                imageSize.set(imageSizes.first())
            imageQualities.setAll(MODEL_INFO[it]?.qualities ?: listOf())
            if (imageQuality.value !in imageQualities)
                imageQuality.set(imageQualities.firstOrNull())
            imageStyles.setAll(MODEL_INFO[it]?.styles ?: listOf())
            if (imageStyle.value !in imageStyles)
                imageStyle.set(imageStyles.firstOrNull())
            countSlider.min = MODEL_INFO[it]?.counts?.first?.toDouble() ?: 1.0
            countSlider.max = MODEL_INFO[it]?.counts?.last?.toDouble() ?: 1.0
            countSlider.isDisable = countSlider.min == countSlider.max
        }
    }
    /** Available sizes based on model */
    private val imageSizes: ObservableList<String> = observableListOf(MODEL_INFO[model.value]?.sizes ?: listOf("1024x1024"))
    /** Available quality values based on model */
    private val imageQualities: ObservableList<String> = observableListOf(MODEL_INFO[model.value]?.qualities ?: listOf())
    /** Available styles based on model */
    private val imageStyles: ObservableList<String> = observableListOf(MODEL_INFO[model.value]?.styles ?: listOf())

    /** Image size */
    private val imageSize = SimpleObjectProperty(imageSizes.first())
    /** Image quality */
    private val imageQuality = SimpleObjectProperty(imageQualities.firstOrNull())
    /** Image style */
    private val imageStyle = SimpleObjectProperty(imageStyles.firstOrNull())
    /** Number of images to generate */
    private val imageCount = SimpleIntegerProperty(1)

    /** Slider used for count. */
    private lateinit var countSlider: Slider
    /** Grid thumbnail size */
    private val thumbnailSize = SimpleDoubleProperty(128.0)

    init {
        addInputTextArea(input)
    }

    init {
        with(outputPane) {
            clear()
            toolbar {
                button("Save All...") {
                    enableWhen { Bindings.isNotEmpty(images) }
                    action { saveAllToFile() }
                }
            }
            datagrid(images) {
                vgrow = Priority.ALWAYS
                prefWidth = 600.0
                prefHeight = 600.0
                cellWidthProperty.bind(thumbnailSize)
                cellHeightProperty.bind(thumbnailSize)
                cellCache {
                    imageview(it.firstValue.imageContent()!!.base64ToImage()) {
                        fitWidthProperty().bind(thumbnailSize)
                        fitHeightProperty().bind(thumbnailSize)
                        isPreserveRatio = true
                        isPickOnBounds = true // so you can click anywhere on transparent images
                        tooltip { graphic = vbox {
                            val text = text(it.prompt!!.template) {
                                style = "-fx-fill: white;"
                            }
                            val image = imageview(it.firstValue.imageContent()!!.base64ToImage())
                            text.wrappingWidthProperty().bind(image.image.widthProperty())
                        } }
                        contextmenu {
                            item("View full size").action { showImageDialog(image) }
                            item("Copy to clipboard").action { copyToClipboard(image) }
                            item("Copy prompt to clipboard").action { copyPromptToClipboard(it) }
                            item("Save to file...").action { saveToFile(image) }
                            separator()
                            item("Remove").action { images.remove(it) }
                        }
                    }
                }
            }
        }
    }

    init {
        parameters("Options") {
            field("Model") {
                combobox(model, imageModelIds)
            }
            field("# Images") {
                countSlider = slider(1..10) {
                    valueProperty().bindBidirectional(imageCount)
                }
                label(imageCount)
            }
            field ("Size") {
                combobox(imageSize, imageSizes)
            }
            field("Quality") {
                enableWhen(imageQuality.isNotNull)
                combobox(imageQuality, imageQualities) {
                    cellFormat { text = it ?: "N/A" }
                }
            }
            field("Style") {
                enableWhen(imageStyle.isNotNull)
                combobox(imageStyle, imageStyles) {
                    cellFormat { text = it ?: "N/A" }
                }
            }
        }
        parameters("Output") {
            field("Thumbnail Size") {
                slider(64.0..256.0) {
                    valueProperty().bindBidirectional(thumbnailSize)
                }
                label(thumbnailSize.integerBinding { it!!.toInt() })
            }
        }
    }

    init {
        onCompleted {
            val fr = it.finalResult as AiImageTrace
            if (fr.exec.error != null) {
                error("Error: ${fr.exec.error}")
            } else {
                runLater {
                    images.addAll(fr.splitImages())
                }
            }
        }
    }

    override fun plan() = aitask("generate-image") {
        val t0 = System.currentTimeMillis()
        val promptInfo = PromptInfo(input.value)
        val modelInfo = AiModelInfo(
            model.value, modelParams = mapOfNotNull(
                "n" to imageCount.value,
                "size" to imageSize.value,
                "quality" to imageQuality.value,
                "style" to imageStyle.value
            )
        )
        val result = try {
            val generator = PromptFxModels.imageModels().find { it.modelId == model.value }
                ?: throw IllegalStateException("No image generator found for model: ${model.value}")
            val params = ImageGenerationParams(
                size = imageSize.value,
                numResponses = imageCount.value,
                quality = imageQuality.value,
                style = imageStyle.value
            )
            val uris = generator.generateImage(input.value, params)
            val outputs = uris.map { uri ->
                val base64 = uri.toString().substringAfter(";base64,")
                AiOutput(multimodalMessage = MultimodalChatMessage.imageBase64(imageBase64 = base64))
            }
            AiImageTrace(
                promptInfo, modelInfo,
                AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                AiOutputInfo(outputs)
            )
        } catch (x: Exception) {
            AiImageTrace(promptInfo, modelInfo, AiExecInfo.error(x.message))
        }
        result
    }.planner

    //region CONTEXT MENU ACTIONS

    private fun copyPromptToClipboard(trace: AiImageTrace) {
        clipboard.putString(trace.prompt!!.template)
    }

    private fun saveAllToFile() {
        promptFxDirectoryChooser(
            dirKey = PromptFxConfig.Companion.DIR_KEY_IMAGE,
            title = "Save Images to Folder"
        ) { folder ->
            var i = 1
            var success = 0
            images.forEach { trace ->
                (trace.values ?: listOf()).forEach {
                    val image = it.imageContent()?.base64ToImage()
                        ?: run {
                            // skip outputs with no renderable image content
                            return@forEach
                        }
                    var file = folder.resolve("image-$i.png")
                    while (file.exists()) {
                        file = folder.resolve("image-${++i}.png")
                    }
                    if (writeImageToFile(image, file))
                        success++
                }
            }
            information("Saved $success images to folder: ${folder.name}", owner = primaryStage)
        }
    }

    //endregion

    //region DRAG & DROP IMAGES FOR EDIT - TBD

//    private val file = SimpleObjectProperty<File?>(null)
//        with(inputPane) {
//            (getChildList()!![0] as TextArea).isEditable = false
//            onDragOver = EventHandler {
//                if (it.dragboard.hasFiles())
//                    it.acceptTransferModes(TransferMode.COPY)
//            }
//            onDragDropped = EventHandler {
//                it.dragboard.files.firstOrNull()?.let { dropImageFile(it) }
//            }
//        }
//    private fun dropImageFile(f: File) {
//        file.set(f)
//        input.value = "Image file: ${f.name}"
//    }

//    override suspend fun processUserInput(): MessageWithTokens {
//        val result = when (val f = file.value) {
//            null ->
//                openaiclient.imageJSON(ImageCreation(
//                    prompt = input.get(),
//                    size = ImageSize.is512x512
//                ))
//            else -> openaiclient.imageJSON(ImageVariation(
//                image = FileSource(f.toOkioPath(), FileSystem.SYSTEM),
//                size = ImageSize.is512x512,
//            ))
//        }
//        return MessageWithTokens(result[0].b64JSON, 0)
//    }

    //endregion

    companion object {
        private const val DALLE2_ID = "dall-e-2"
        private const val DALLE3_ID = "dall-e-3"
        private const val GPT_IMAGE1 = "gpt-image-1"
        private const val GPT_IMAGE1_MINI = "gpt-image-1-mini"
        private const val GEMINI_IMAGE_ID = "gemini-2.5-flash-image"

        val MODEL_INFO = mapOf(
            DALLE2_ID to ImageModelCapabilities(DALLE2_ID,
                sizes = listOf("256x256", "512x512", "1024x1024"),
                qualities = listOf(),
                styles = listOf(),
                counts = 1..10
            ),
            DALLE3_ID to ImageModelCapabilities(DALLE3_ID,
                sizes = listOf("1024x1024", "1792x1024", "1024x1792"),
                qualities = listOf("standard", "hd", "auto"),
                styles = listOf("vivid", "natural"),
                counts = 1..1
            ),
            GPT_IMAGE1 to ImageModelCapabilities(GPT_IMAGE1,
                sizes = listOf("1024x1024", "1536x1024", "1024x1536", "auto"),
                qualities = listOf("low", "medium", "high", "auto"),
                styles = listOf(),
                counts = 1..10
            ),
            GPT_IMAGE1_MINI to ImageModelCapabilities(GPT_IMAGE1_MINI,
                sizes = listOf("1024x1024", "1536x1024", "1024x1536", "auto"),
                qualities = listOf("low", "medium", "high", "auto"),
                styles = listOf(),
                counts = 1..10
            ),
            GEMINI_IMAGE_ID to ImageModelCapabilities(GEMINI_IMAGE_ID,
                sizes = listOf("1:1", "9:16", "16:9", "3:4", "4:3"),
                qualities = listOf(),
                styles = listOf(),
                counts = 1..1
            )
        )

        private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
            mapOf(*pairs).filterValues { it != null } as Map<String, Any>
    }

}

/** Image model capabilities for specific models. All options use plain strings for provider-agnostic use. */
class ImageModelCapabilities(
    val modelId: String,
    /** Size options: pixel dimensions (e.g. "1024x1024") for OpenAI, aspect ratios (e.g. "1:1") for Gemini. */
    val sizes: List<String>,
    /** Quality options (model-specific, e.g. "standard", "hd", "auto"). Empty if not supported. */
    val qualities: List<String>,
    /** Style options (model-specific, e.g. "vivid", "natural"). Empty if not supported. */
    val styles: List<String>,
    val counts: IntRange
)
