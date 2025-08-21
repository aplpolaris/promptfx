package tri.promptfx.multimodal

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import tornadofx.*
import tri.ai.core.MChatRole
import tri.ai.core.VisionLanguageChatMessage
import tri.ai.pips.task
import tri.promptfx.AiPlanTaskView
import tri.promptfx.PromptFxGlobals.promptsWithPrefix
import tri.promptfx.PromptFxModels
import tri.promptfx.ui.PromptSelectionModel
import tri.promptfx.ui.promptfield
import tri.util.ui.NavigableWorkspaceViewImpl
import tri.util.ui.imageUri
import java.net.URI

/** Plugin for the [ImageDescribeView]. */
class ImageDescribePlugin : NavigableWorkspaceViewImpl<ImageDescribeView>("Multimodal", "Image Description", type = ImageDescribeView::class)

/** View designed to describe an image. */
class ImageDescribeView: AiPlanTaskView("Image Description (beta)", "Drop an image to describe into the box on the left.") {

    companion object {
        private const val PROMPT_PREFIX = "image-describe"
    }

    private val image = SimpleObjectProperty<Image>(null)
    private val model = SimpleObjectProperty(PromptFxModels.visionLanguageModelDefault())

    private val prompt = PromptSelectionModel("$PROMPT_PREFIX/basic")

    init {
        addInputImageArea(image)
        parameters("Vision Language Model") {
            field("Model") {
                combobox(model, PromptFxModels.visionLanguageModels())
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

    override fun plan() = task("Describe Image") {
        describeImage(prompt.text.value)
    }.planner

    fun setImage(image: Image) {
        this.image.value = image
    }

    private suspend fun describeImage(prompt: String): String? {
        val res = model.value.chat(
            listOf(
                VisionLanguageChatMessage(MChatRole.User, prompt, URI.create(image.value.imageUri()))
            ),
            common.temp.value,
            common.maxTokens.value,
            null,
            false
        )
        return res.firstValue.content!!
    }

}