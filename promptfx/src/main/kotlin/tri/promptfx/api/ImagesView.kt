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

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.stage.StageStyle
import tornadofx.*
import tri.ai.pips.AiPipelineResult
import tri.ai.pips.AiTaskResult.Companion.result
import tri.promptfx.AiTaskView

/** View for the OpenAI API's image endpoint. */
@OptIn(BetaOpenAI::class)
class ImagesView : AiTaskView("Images", "Enter image prompt") {

    private val input = SimpleStringProperty("")
    /** Image URLs */
    private val images = observableListOf<String>()

    private val numProperty = SimpleIntegerProperty(1)
    private val imageSize = SimpleObjectProperty(ImageSize.is256x256)

    private val IMAGE_SIZES = listOf(ImageSize.is256x256, ImageSize.is512x512, ImageSize.is1024x1024)

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
                        setOnMouseClicked {
                            dialog(
                                modality = Modality.APPLICATION_MODAL,
                                stageStyle = StageStyle.UNDECORATED,
                                owner = primaryStage
                            ) {
                                form.padding = null
                                padding = null
                                imageview(image) {
                                    onLeftClick { close() }
                                }

                            }
                        }
                    }
                }
            }
        }
        parameters("Options") {
            field("# Images") {
                slider(1..10) {
                    valueProperty().bindBidirectional(numProperty)
                }
                label(numProperty)
            }
            field ("Size") {
                combobox(imageSize, IMAGE_SIZES)
            }
        }
    }

    override suspend fun processUserInput(): AiPipelineResult {
        val result = controller.openAiPlugin.client.imageURL(ImageCreation(
            prompt = input.get(),
            n = numProperty.get(),
            size = imageSize.get()
        ))
        images.addAll(result.value ?: listOf())
        return result(result.value?.get(0) ?: "No images created", "DALL-E").asPipelineResult()
    }

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

}
