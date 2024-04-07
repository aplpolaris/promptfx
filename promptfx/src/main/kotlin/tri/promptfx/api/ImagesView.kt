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
package tri.promptfx.api

import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.input.DataFormat
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.stage.StageStyle
import tornadofx.*
import tri.ai.openai.OpenAiModels
import tri.ai.pips.AiTaskResult.Companion.result
import tri.ai.pips.aitask
import tri.ai.prompt.trace.AiPromptExecInfo
import tri.ai.prompt.trace.AiPromptInfo
import tri.ai.prompt.trace.AiPromptModelInfo
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxConfig
import tri.promptfx.promptFxDirectoryChooser
import tri.promptfx.promptFxFileChooser
import tri.util.loggerFor
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.warning
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

/** Plugin for the [ImagesView]. */
class ImagesApiPlugin : NavigableWorkspaceViewImpl<ImagesView>("Vision", "Text-to-Image", ImagesView::class)

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
                    imageQualities.setAll(STANDARD, HD)
                }
                DALLE2_ID -> {
                    imageQualities.setAll(STANDARD)
                    quality.set(STANDARD)
                }
                else ->
                    throw UnsupportedOperationException("Unsupported model: $it")
            }
        }
    }
    /** Available sizes based on model */
    private val imageSizes: ObservableList<ImageSize> = observableListOf(IMAGE_SIZES[model.value] ?: listOf())
    /** Available quality values based on model */
    private val imageQualities: ObservableList<String> = observableListOf(STANDARD)

    /** Number of images to generate */
    private val numProperty = SimpleIntegerProperty(1)
    /** Image size */
    private val imageSize = SimpleObjectProperty(ImageSize.is256x256)
    /** Image quality */
    private val quality = SimpleStringProperty(STANDARD)
    /** Image style */
    private val imageStyle = SimpleStringProperty(VIVID)

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
                    imageview(it.outputInfo.imageUrls.first()) {
                        fitWidthProperty().bind(thumbnailSize)
                        fitHeightProperty().bind(thumbnailSize)
                        isPreserveRatio = true
                        tooltip { graphic = vbox {
                            label(it.promptInfo.prompt)
                            imageview(it.outputInfo.imageUrls.first())
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
                tooltip("Not yet supported by API")
                isDisable = true // TODO - enable when API supports this
//                enableWhen { model.isEqualTo(DALLE3_ID) }
                combobox(quality, imageQualities)
            }
            field("Style") {
                tooltip("Not yet supported by API")
                isDisable = true // TODO - enable when API supports this
//                enableWhen { model.isEqualTo(DALLE3_ID) }
                combobox(imageStyle, listOf(VIVID, NATURAL))
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
            if (fr.execInfo.error != null) {
                error("Error: ${fr.execInfo.error}")
            } else {
                images.addAll(fr.splitImages())
            }
        }
    }

    override fun plan() = aitask("generate-image") {
        val t0 = System.currentTimeMillis()
        val promptInfo = AiPromptInfo(input.value)
        val modelInfo = AiPromptModelInfo(model.value, mapOf(
            "n" to numProperty.value,
            "size" to imageSize.value,
            "quality" to quality.value,
            "style" to imageStyle.value
        ))
        val result = try {
            val images = controller.openAiPlugin.client.imageURL(
                ImageCreation(
                    model = ModelId(model.value),
                    prompt = input.value,
                    n = numProperty.value,
                    size = imageSize.value,
//            quality = quality.value,
//            style = imageStyle.value
                )
            )
            AiImageTrace(promptInfo, modelInfo,
                AiPromptExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                AiImageOutputInfo(images.value!!)
            )
        } catch (x: OpenAIAPIException) {
            AiImageTrace(promptInfo, modelInfo, AiPromptExecInfo.error(x.message))
        }
        result(result, model.value)
    }.planner

    //region CONTEXT MENU ACTIONS

    private fun showImageDialog(image: Image) {
        val d = dialog(
            modality = Modality.APPLICATION_MODAL,
            stageStyle = StageStyle.UNDECORATED,
            owner = primaryStage
        ) {
            imageview(image) {
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

    private fun copyToClipboard(image: Image) {
        // the original image doesn't seem to copy to clipboard properly, so cycle it through [BufferedImage]
        val image2 = SwingFXUtils.fromFXImage(image, null)
        val fxImage = SwingFXUtils.toFXImage(image2, null)
        clipboard.put(DataFormat.IMAGE, fxImage)
    }

    private fun copyPromptToClipboard(trace: AiImageTrace) {
        clipboard.putString(trace.promptInfo.prompt)
    }

    private fun saveToFile(image: Image) {
        promptFxFileChooser(
            dirKey = PromptFxConfig.DIR_KEY_IMAGE,
            title = "Save to File",
            filters = arrayOf(PromptFxConfig.FF_PNG, PromptFxConfig.FF_ALL),
            mode = FileChooserMode.Save
        ) {
            it.firstOrNull()?.let {
                writeImageToFile(image, it)
                information("Image saved to file: ${it.name}", owner = primaryStage)
            }
        }
    }

    private fun saveAllToFile() {
        promptFxDirectoryChooser(
            dirKey = PromptFxConfig.DIR_KEY_IMAGE,
            title = "Save Images to Folder"
        ) { folder ->
            var i = 1
            var success = 0
            images.forEach { trace ->
                trace.outputInfo.imageUrls.forEach {
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

    private fun writeImageToFile(image: Image, file: File): Boolean = try {
        file.outputStream().use { os ->
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), file.extension, os)
        }
        true
    } catch (x: IOException) {
        loggerFor<ImagesView>().warning("Error saving image to file: $file", x)
        false
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

        // TODO - expect these will be replaced by enums in API
        private const val STANDARD = "standard"
        private const val HD = "hd"
        private const val VIVID = "vivid"
        private const val NATURAL = "natural"

        private val IMAGE_MODELS = OpenAiModels.visionModels()
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
            )
        )
    }

}

/**
 * Details of an executed image prompt, including prompt configuration, model configuration, execution metadata, and output.
 * Not designed for serialization (yet).
 */
class AiImageTrace(
    var promptInfo: AiPromptInfo,
    var modelInfo: AiPromptModelInfo,
    var execInfo: AiPromptExecInfo = AiPromptExecInfo(),
    var outputInfo: AiImageOutputInfo = AiImageOutputInfo(listOf())
) {
    /** Unique identifier for this trace. */
    var uuid = UUID.randomUUID().toString()

    override fun toString() = "AiImageTrace(uuid='$uuid', promptInfo=$promptInfo, modelInfo=$modelInfo, execInfo=$execInfo, outputInfo=$outputInfo)"

    /** Splits this image trace into individual images. */
    fun splitImages(): List<AiImageTrace> =
        outputInfo.imageUrls.map {
            AiImageTrace(promptInfo, modelInfo, execInfo, AiImageOutputInfo(listOf(it)))
        }
}

/** Output info for an image prompt. */
class AiImageOutputInfo(
    var imageUrls: List<String>
)
