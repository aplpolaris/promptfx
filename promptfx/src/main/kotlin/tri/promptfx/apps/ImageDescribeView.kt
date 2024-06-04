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

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.image.Image
import tornadofx.combobox
import tornadofx.field
import tornadofx.stringBinding
import tornadofx.tooltip
import tri.ai.core.TextChatRole
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.pips.task
import tri.ai.prompt.AiPromptLibrary
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxModels
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.WorkspaceViewAffordance
import tri.util.ui.toUri
import java.net.URI

/** Plugin for the [ImageDescribeView]. */
class ImageDescribePlugin : NavigableWorkspaceViewImpl<ImageDescribeView>("Vision", "Image Description", type = ImageDescribeView::class)

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

    override fun plan() = task("Describe Image") {
        describeImage(promptText.value)
    }.planner

    private suspend fun describeImage(prompt: String): String? {
        val res = model.value.chat(
            listOf(
                VisionLanguageChatMessage(TextChatRole.User, prompt, URI.create(image.value.toUri()))
            ),
            common.temp.value,
            common.maxTokens.value,
            null,
            false
        )
        return res.value!!.content!!
    }

}
