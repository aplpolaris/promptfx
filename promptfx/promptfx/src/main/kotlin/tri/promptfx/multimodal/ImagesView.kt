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
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import tornadofx.*
import tri.ai.core.AiModel
import tri.ai.core.ImageGenerationParams
import tri.ai.core.ImageGenerator
import tri.ai.core.MultimodalChatMessage
import tri.ai.gemini.GEMINI_ASPECT_RATIOS
import tri.ai.gemini.GEMINI_IMAGE_SIZES
import tri.ai.pips.AiTaskBuilder
import tri.ai.prompt.trace.*
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxModels
import tri.promptfx.promptFxDirectoryChooser
import tri.util.ui.*

/** Plugin for the [ImagesView]. */
class ImagesApiPlugin : NavigableWorkspaceViewImpl<ImagesView>("Multimodal", "Text-to-Image", WorkspaceViewAffordance.Companion.INPUT_ONLY, ImagesView::class)

/** View for image generation, supporting multiple providers (e.g. OpenAI DALL-E, Gemini). */
class ImagesView : AiPlanTaskView("Images", "Enter image prompt") {

    /** User input */
    private val input = SimpleStringProperty("")
    /** Image results */
    private val images = observableListOf<AiTaskTrace>()

    private val imageModels = observableListOf<ImageGenerator>().apply {
        setAll(PromptFxModels.imageModels())
    }

    /** Model */
    private val model = SimpleObjectProperty(PromptFxModels.imageModelDefault()).apply {
        onChange {
            if (it == null) return@onChange
            imageSizes.setAll(modelCapabilities(it)?.sizes ?: listOf("1024x1024"))
            if (imageSize.value !in imageSizes)
                imageSize.set(imageSizes.first())
            imageAspectRatios.setAll(modelCapabilities(it)?.aspectRatios ?: listOf())
            if (imageAspectRatio.value !in imageAspectRatios)
                imageAspectRatio.set(imageAspectRatios.firstOrNull())
            imageQualities.setAll(modelCapabilities(it)?.qualities ?: listOf())
            if (imageQuality.value !in imageQualities)
                imageQuality.set(imageQualities.firstOrNull())
            imageStyles.setAll(modelCapabilities(it)?.styles ?: listOf())
            if (imageStyle.value !in imageStyles)
                imageStyle.set(imageStyles.firstOrNull())
            countSlider.min = modelCapabilities(it)?.counts?.first?.toDouble() ?: 1.0
            countSlider.max = modelCapabilities(it)?.counts?.last?.toDouble() ?: 1.0
            countSlider.isDisable = countSlider.min == countSlider.max
        }
    }
    /** Available sizes based on model */
    private val imageSizes: ObservableList<String> = observableListOf(modelCapabilities(model.value)?.sizes ?: listOf("1024x1024"))
    /** Available aspect ratios based on model (empty if not applicable) */
    private val imageAspectRatios: ObservableList<String> = observableListOf(modelCapabilities(model.value)?.aspectRatios ?: listOf())
    /** Available quality values based on model */
    private val imageQualities: ObservableList<String> = observableListOf(modelCapabilities(model.value)?.qualities ?: listOf())
    /** Available styles based on model */
    private val imageStyles: ObservableList<String> = observableListOf(modelCapabilities(model.value)?.styles ?: listOf())

    /** Image size */
    private val imageSize = SimpleObjectProperty(imageSizes.first())
    /** Image aspect ratio (null if not applicable for model) */
    private val imageAspectRatio = SimpleObjectProperty(imageAspectRatios.firstOrNull())
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
                    ImageView(it.firstValue.imageContent()!!.base64ToImage()).apply {
                        fitWidthProperty().bind(thumbnailSize)
                        fitHeightProperty().bind(thumbnailSize)
                        isPreserveRatio = true
                        isPickOnBounds = true // so you can click anywhere on transparent images
                        tooltip { graphic = VBox().apply {
                            val text = text(it.input?.prompt ?: "") {
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
                combobox(model, imageModels)
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
            field("Aspect Ratio") {
                enableWhen(imageAspectRatio.isNotNull)
                combobox(imageAspectRatio, imageAspectRatios)
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
            val fr = it.finalResult
            if (fr.exec.error != null) {
                error("Error: ${fr.exec.error}")
            } else {
                runLater {
                    images.addAll(fr.splitImages())
                }
            }
        }
    }

    override fun plan() = AiTaskBuilder.task("generate-image") { context ->
        val t0 = System.currentTimeMillis()
        val promptInfo = PromptInfo(input.value)
        val generator = model.value!!
        val modelInfo = AiModelInfo(
            generator.modelId, generator.modelSource, modelParams = mapOfNotNull(
                "size" to imageSize.value,
                "aspect_ratio" to imageAspectRatio.value,
                "quality" to imageQuality.value,
                "style" to imageStyle.value,
                "n" to imageCount.value
            )
        )
        val result = try {
            val params = ImageGenerationParams(
                size = imageSize.value,
                aspectRatio = imageAspectRatio.value,
                numResponses = imageCount.value,
                quality = imageQuality.value,
                style = imageStyle.value
            )
            val uris = generator.generateImage(input.value, params)
            val outputs = uris.map { uri ->
                val base64 = uri.toString().substringAfter(";base64,")
                AiOutput.MultimodalMessage(MultimodalChatMessage.imageBase64(imageBase64 = base64))
            }
            AiTaskTrace(
                env = AiEnvInfo.of(modelInfo),
                input = AiTaskInputInfo.of(promptInfo),
                exec = AiExecInfo.durationSince(t0),
                output = AiOutputInfo(outputs)
            )
        } catch (x: Exception) {
            AiTaskTrace(
                env = AiEnvInfo.of(modelInfo),
                input = AiTaskInputInfo.of(promptInfo),
                exec = AiExecInfo.error(x.message, x)
            )
        }
        context.logTrace("generate-image", result)
        result.values ?: emptyList()
    }

    //region CONTEXT MENU ACTIONS

    private fun copyPromptToClipboard(trace: AiTaskTrace) {
        clipboard.putString(trace.input?.prompt ?: "")
    }

    private fun saveAllToFile() {
        promptFxDirectoryChooser(
            dirKey = PromptFxConfig.DIR_KEY_IMAGE,
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
        private const val GEMINI_SOURCE = "Gemini"
        private const val GEMINI_SDK_SOURCE = "Gemini-SDK"

        private val MODEL_CAPABILITY_INFO = mapOf(
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
            GEMINI_SOURCE to ImageModelCapabilities(GEMINI_SOURCE,
                sizes = GEMINI_IMAGE_SIZES,
                aspectRatios = GEMINI_ASPECT_RATIOS,
                qualities = listOf(),
                styles = listOf(),
                counts = 1..10
            ),
            GEMINI_SDK_SOURCE to ImageModelCapabilities(GEMINI_SDK_SOURCE,
                sizes = GEMINI_IMAGE_SIZES,
                aspectRatios = GEMINI_ASPECT_RATIOS,
                qualities = listOf(),
                styles = listOf(),
                counts = 1..10
            )
        )

        /** Looks up capabilities by model ID first, then falls back to model source. */
        private fun modelCapabilities(model: AiModel?) =
            if (model == null) null
            else MODEL_CAPABILITY_INFO[model.modelId] ?: MODEL_CAPABILITY_INFO[model.modelSource]

        private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
            mapOf(*pairs).filterValues { it != null } as Map<String, Any>
    }

}

/** Image model capabilities for specific models. All options use plain strings for provider-agnostic use. */
class ImageModelCapabilities(
    val modelId: String,
    /** Size options: pixel dimensions (e.g. "1024x1024") for OpenAI, image size codes (e.g. "1K") for Gemini. */
    val sizes: List<String>,
    /** Aspect ratio options (e.g. "1:1", "16:9"). Used by Gemini; empty for OpenAI which uses [sizes] for dimensions. */
    val aspectRatios: List<String> = listOf(),
    /** Quality options (model-specific, e.g. "standard", "hd", "auto"). Empty if not supported. */
    val qualities: List<String>,
    /** Style options (model-specific, e.g. "vivid", "natural"). Empty if not supported. */
    val styles: List<String>,
    val counts: IntRange
)
