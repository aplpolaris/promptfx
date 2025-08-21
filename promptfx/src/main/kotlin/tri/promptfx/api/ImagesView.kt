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
package tri.promptfx.api

import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.image.Quality
import com.aallam.openai.api.image.Style
import com.aallam.openai.api.model.ModelId
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import tornadofx.*
import tri.ai.core.ImageSize as CoreImageSize
import tri.ai.openai.OpenAiModelIndex
import tri.ai.pips.aitask
import tri.ai.prompt.trace.*
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.PromptFxModels
import tri.promptfx.promptFxDirectoryChooser
import tri.util.ui.*

/** Plugin for the [ImagesView]. */
class ImagesApiPlugin : NavigableWorkspaceViewImpl<ImagesView>("Vision", "Text-to-Image", WorkspaceViewAffordance.INPUT_ONLY, ImagesView::class)

/** View for the OpenAI API's image endpoint (https://platform.openai.com/docs/api-reference/images). */
class ImagesView : AiPlanTaskView("Images", "Enter image prompt") {

    /** User input */
    private val input = SimpleStringProperty("")
    /** Image results */
    private val images = observableListOf<AiImageTrace>()

    /** Model */
    private val model = SimpleStringProperty(IMAGE_MODELS.first()).apply {
        onChange {
            imageSizes.setAll(IMAGE_SIZES[it] ?: listOf())
            if (imageSize.value !in imageSizes)
                imageSize.set(imageSizes.first())
            when (it) {
                DALLE3_ID -> {
                    numProperty.set(1)
                    imageQualities.setAll(STANDARD_QUALITY, Quality.HD)
                    quality.set(STANDARD_QUALITY)
                }
                DALLE2_ID -> {
                    imageQualities.setAll(STANDARD_QUALITY)
                    quality.set(STANDARD_QUALITY)
                }
                IMAGEN_3_GENERATE, IMAGEN_3_FAST_GENERATE -> {
                    numProperty.set(1)
                    imageQualities.setAll(STANDARD_QUALITY)
                    quality.set(STANDARD_QUALITY)
                }
                else ->
                    throw UnsupportedOperationException("Unsupported model: $it")
            }
        }
    }
    /** Available sizes based on model */
    private val imageSizes: ObservableList<ImageSize> = observableListOf(IMAGE_SIZES[model.value] ?: listOf())
    /** Available quality values based on model */
    private val imageQualities = observableListOf(STANDARD_QUALITY, Quality.HD)
    /** Available styles based on model */
    private val imageStyles = observableListOf(Style.Vivid, Style.Natural)

    /** Number of images to generate */
    private val numProperty = SimpleIntegerProperty(1)
    /** Image size */
    private val imageSize = SimpleObjectProperty(ImageSize.is256x256)
    /** Image quality */
    private val quality = SimpleObjectProperty(STANDARD_QUALITY)
    /** Image style */
    private val imageStyle = SimpleObjectProperty(Style.Vivid)

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
                    imageview(it.firstValue) {
                        fitWidthProperty().bind(thumbnailSize)
                        fitHeightProperty().bind(thumbnailSize)
                        isPreserveRatio = true
                        isPickOnBounds = true // so you can click anywhere on transparent images
                        tooltip { graphic = vbox {
                            val text = text(it.prompt!!.prompt) {
                                style = "-fx-fill: white;"
                            }
                            val image = imageview(it.firstValue)
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
                combobox(model, IMAGE_MODELS)
            }
            field("# Images") {
                disableWhen { model.isEqualTo(DALLE3_ID) } // dall-e-3 requires n=1
                slider(1..10) {
                    valueProperty().bindBidirectional(numProperty)
                }
                label(numProperty)
            }
            field ("Size") {
                combobox(imageSize, imageSizes) {
                    cellFormat { text = it.size }
                }
            }
            field("Quality") {
                enableWhen { model.isEqualTo(DALLE3_ID) }
                combobox(quality, imageQualities) {
                    cellFormat { text = it.value }
                }
            }
            field("Style") {
                enableWhen { model.isEqualTo(DALLE3_ID) }
                combobox(imageStyle, imageStyles) {
                    cellFormat { text = it.value }
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
        val promptInfo = AiPromptInfo(input.value)
        val modelInfo = AiModelInfo(model.value, mapOf(
            "n" to numProperty.value,
            "size" to imageSize.value,
            "quality" to quality.value,
            "style" to imageStyle.value
        ))
        val result = try {
            // Find the appropriate image generator for this model
            val imageGenerator = PromptFxModels.imageModels().find { it.modelId == model.value }
                ?: throw UnsupportedOperationException("No image generator found for model: ${model.value}")
            
            // Use the generic ImageGenerator interface
            val imageUrls = imageGenerator.generateImage(
                text = input.value,
                size = parseImageSize(imageSize.value),
                numResponses = numProperty.value
            )
            
            AiImageTrace(promptInfo, modelInfo,
                AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                AiOutputInfo(imageUrls.map { it.toString() })
            )
        } catch (x: Exception) {
            AiImageTrace(promptInfo, modelInfo, AiExecInfo.error(x.message))
        }
        result
    }.planner

    //region CONTEXT MENU ACTIONS

    private fun copyPromptToClipboard(trace: AiImageTrace) {
        clipboard.putString(trace.prompt!!.prompt)
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
                    val image = Image(it)
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

    //region HELPER METHODS
    
    /** Convert OpenAI ImageSize to core ImageSize */
    private fun parseImageSize(openAiSize: ImageSize): CoreImageSize {
        val sizeStr = openAiSize.size
        return when (sizeStr) {
            "256x256" -> CoreImageSize(256, 256)
            "512x512" -> CoreImageSize(512, 512)
            "1024x1024" -> CoreImageSize(1024, 1024)
            "1792x1024" -> CoreImageSize(1792, 1024)
            "1024x1792" -> CoreImageSize(1024, 1792)
            else -> {
                // Try to parse generic WxH format
                val parts = sizeStr.split("x")
                if (parts.size == 2) {
                    CoreImageSize(parts[0].toInt(), parts[1].toInt())
                } else {
                    throw UnsupportedOperationException("Unknown image size: $sizeStr")
                }
            }
        }
    }

    //endregion

    companion object {
        private const val DALLE2_ID = "dall-e-2"
        private const val DALLE3_ID = "dall-e-3"
        private const val IMAGEN_3_GENERATE = "imagen-3.0-generate-001"
        private const val IMAGEN_3_FAST_GENERATE = "imagen-3.0-fast-generate-001"

        private val STANDARD_QUALITY = Quality("standard")

        private val IMAGE_MODELS = PromptFxModels.imageModels().map { it.modelId }
        private val IMAGE_SIZES = mapOf(
            DALLE2_ID to listOf(
                ImageSize.is256x256,
                ImageSize.is512x512,
                ImageSize.is1024x1024
            ),
            DALLE3_ID to listOf(
                ImageSize.is1024x1024,
                ImageSize("1792x1024"),
                ImageSize("1024x1792")
            ),
            IMAGEN_3_GENERATE to listOf(
                ImageSize.is1024x1024,
                ImageSize("1792x1024"), 
                ImageSize("1024x1792")
            ),
            IMAGEN_3_FAST_GENERATE to listOf(
                ImageSize.is1024x1024,
                ImageSize("1792x1024"),
                ImageSize("1024x1792")
            )
        )
    }

}
