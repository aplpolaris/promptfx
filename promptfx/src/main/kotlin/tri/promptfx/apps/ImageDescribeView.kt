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
package tri.promptfx.apps

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import tornadofx.*
import tri.ai.openai.OpenAiClient
import tri.ai.pips.task
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.*
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO


/** Plugin for the [ImageDescribeView]. */
class ImageDescribePlugin : NavigableWorkspaceViewImpl<ImageDescribeView>("Vision", "Image Description", ImageDescribeView::class)

/** View designed to describe an image. */
class ImageDescribeView: AiPlanTaskView("Image Description (beta)", "Drop an image to describe into the box on the left.") {

    companion object {
        private const val PROMPT_PREFIX = "image-describe"
    }

    private val image = SimpleObjectProperty<Image>(null)
    private val model = SimpleObjectProperty(PromptFxModels.visionLanguageModelDefault())

    private val promptId = SimpleStringProperty("$PROMPT_PREFIX-basic")
    private val promptText = promptId.stringBinding { AiPromptLibrary.lookupPrompt(it!!).template }

    init {
        addInputImageArea(image)
        parameters("Vision Language Model") {
            field("Model") {
                combobox(model, PromptFxModels.visionLanguageModels())
            }
        }
        parameters("Prompt") {
            tooltip("Loads from prompts.yaml with prefix $PROMPT_PREFIX")
            promptfield("Prompt", promptId, AiPromptLibrary.withPrefix(PROMPT_PREFIX), promptText, workspace)
        }
        parameters("Model Parameters") {
            with (common) {
                temperature()
                maxTokens()
            }
        }
    }

    private val imageBase64
        get() = image.value?.let {
            val bufferedImage = SwingFXUtils.fromFXImage(it, null)
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.getEncoder().encodeToString(byteArray)
        }

    override fun plan() = task("Describe Image") {
        describeImage(promptText.value)
    }.planner

    private suspend fun describeImage(prompt: String): String? {
        val res = OpenAiClient.INSTANCE.client.chatCompletion(
            chatCompletionRequest {
                model = ModelId(this@ImageDescribeView.model.value.modelId)
                temperature = common.temp.value
                maxTokens = common.maxTokens.value
                messages {
                    user {
                        content {
                            text(prompt)
                            image("data:image/png;base64,"+imageBase64!!)
                        }
                    }
                }
            }
        )
        return res.choices[0].message.content
    }

}
