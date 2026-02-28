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

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import tornadofx.*
import tri.ai.core.MChatParameters
import tri.ai.core.MChatVariation
import tri.ai.core.MultimodalChat
import tri.ai.core.chatMessage
import tri.ai.pips.tasktext
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxGlobals.promptsWithPrefix
import tri.promptfx.PromptFxModels
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.imageUri

/** Plugin for the [ImageDescribeView]. */
class ImageDescribePlugin : NavigableWorkspaceViewImpl<ImageDescribeView>("Multimodal", "Image Description", type = ImageDescribeView::class)

/** View designed to describe an image. */
class ImageDescribeView: AiPlanTaskView("Image Description", "Drop an image to describe into the box on the left.") {

    companion object {
        private const val PROMPT_PREFIX = "image-describe"
    }

    private val image = SimpleObjectProperty<Image>(null)
    private val model = SimpleObjectProperty<MultimodalChat>(PromptFxModels.multimodalModelDefault())

    private val prompt = PromptSelectionModel("$PROMPT_PREFIX/basic")

    init {
        addInputImageArea(image)
        parameters("Vision Language Model") {
            field("Model") {
                combobox(model, PromptFxModels.multimodalModels())
            }
        }
        parameters("Prompt") {
            tooltip("Loads prompts with prefix $PROMPT_PREFIX")
            promptfield("Prompt", prompt, promptsWithPrefix(PROMPT_PREFIX), workspace)
        }
        parameters("Model Parameters") {
            with (common) {
                temperature()
                maxTokens()
            }
        }
    }

    override fun plan() = tasktext("Describe Image") {
        describeImage(prompt.text.value)
    }.planner

    fun setImage(image: Image) {
        this.image.value = image
    }

    private suspend fun describeImage(prompt: String): String {
        val model = model.value ?: return "No multimodal model available"
        val res = model.chat(
            chatMessage {
                text(prompt)
                image(image.value.imageUri())
            },
            MChatParameters(
                tokens = common.maxTokens.value,
                variation = MChatVariation(temperature = common.temp.value)
            )
        )
        return res.firstValue.textContent()
    }

}
