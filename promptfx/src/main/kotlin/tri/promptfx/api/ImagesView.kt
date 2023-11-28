/*-
 * #%L
 * promptfx-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.stage.StageStyle
import tornadofx.*
import tri.ai.openai.OpenAiModels
import tri.ai.pips.AiTaskResult.Companion.result
import tri.ai.pips.aitask
import tri.promptfx.AiPlanTaskView
import tri.util.ui.NavigableWorkspaceViewImpl

/** Plugin for the [ImagesView]. */
class ImagesApiPlugin : NavigableWorkspaceViewImpl<ImagesView>("Vision", "Text-to-Image", ImagesView::class)

/** View for the OpenAI API's image endpoint (https://platform.openai.com/docs/api-reference/images). */
class ImagesView : AiPlanTaskView("Images", "Enter image prompt") {

    /** User input */
    private val input = SimpleStringProperty("")
    /** Image URLs */
    private val images = observableListOf<String>()

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

    init {
        addInputTextArea(input)
        with (outputPane) {
            val thumbnailSize = 128.0
            clear()
            datagrid(images) {
                vgrow = Priority.ALWAYS
                cellWidth = thumbnailSize
                cellHeight = thumbnailSize
                cellCache {
                    imageview(it) {
                        fitWidth = thumbnailSize
                        fitHeight = thumbnailSize
                        tooltip { graphic = imageview(it) }
                        setOnMouseClicked {
                            dialog(
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
                        }
                    }
                }
            }
        }
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
                combobox(imageSize, imageSizes)
            }
            field("Quality") {
                tooltip("Not yet supported by API")
                isDisable = true // TODO - enable when API supports this
                combobox(quality, imageQualities)
            }
        }
    }

    override fun plan() = aitask("generate-image") {
        val result = controller.openAiPlugin.client.imageURL(ImageCreation(
            model = ModelId(model.value),
            prompt = input.value,
            n = numProperty.value,
            size = imageSize.value,
//            quality = quality.value
        ))
        images.addAll(result.value ?: listOf())
        result(result.value?.get(0) ?: "No images created", "DALL-E")
    }.planner

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

        private val IMAGE_MODELS = OpenAiModels.visionModels()
        private val IMAGE_SIZES = mapOf(
            DALLE2_ID to listOf(
                ImageSize.is256x256,
                ImageSize.is512x512,
                ImageSize.is1024x1024
            ),
            DALLE3_ID to listOf(
                ImageSize.is1024x1024,
// TODO - update when API supports these
//                ImageSize.is1792x1024,
//                ImageSize.is1024x1792
            )
        )
    }

}
