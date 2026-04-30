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
package tri.ai.openai

import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.model.ModelId
import tri.ai.core.ImageGenerationParams
import tri.ai.core.ImageGenerator
import java.net.URI

/** Image generation with OpenAI models. */
class OpenAiImageGenerator(override val modelId: String = OpenAiModelIndex.IMAGE_DALLE2, override val modelSource: String = OpenAiModelIndex.MODEL_SOURCE, val client: OpenAiAdapter = OpenAiAdapter.INSTANCE) :
    ImageGenerator {

    override fun toString() = modelDisplayName()

    override suspend fun generateImage(text: String, params: ImageGenerationParams): List<URI> {
        val imageCreation = ImageCreation(
            model = ModelId(modelId),
            prompt = text,
            n = params.numResponses ?: 1,
            size = params.size?.toOpenAiSize() ?: com.aallam.openai.api.image.ImageSize.is1024x1024,
            quality = params.quality?.let { com.aallam.openai.api.image.Quality(it) },
            style = params.style?.let { com.aallam.openai.api.image.Style(it) }
        )
        val images = if (modelId.startsWith("gpt-image")) {
            client.imageJSONDirect(imageCreation)
        } else {
            client.imageJSON(imageCreation)
        }
        return images.output!!.outputs.map { output ->
            val base64 = output.imageContent()
                ?: throw IllegalStateException("No image content in response for model $modelId")
            URI("data:image/png;base64,$base64")
        }
    }

    private fun String.toOpenAiSize() = when (this) {
        "256x256" -> com.aallam.openai.api.image.ImageSize.is256x256
        "512x512" -> com.aallam.openai.api.image.ImageSize.is512x512
        "1024x1024" -> com.aallam.openai.api.image.ImageSize.is1024x1024
        else -> com.aallam.openai.api.image.ImageSize(this)
    }

}
