/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.openai

import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.model.ModelId
import tri.ai.core.ImageGenerator
import tri.ai.core.ImageSize
import java.net.URL

/** Image generation with OpenAI models. */
class OpenAiImageGenerator(override val modelId: String = OpenAiModelIndex.IMAGE_DALLE2, val client: OpenAiClient = OpenAiClient.INSTANCE) :
    ImageGenerator {

    override fun toString() = modelId

    override suspend fun generateImage(text: String, size: ImageSize, prompt: String?, numResponses: Int?): List<URL> {
        val images = client.imageURL(
            ImageCreation(
                model = ModelId(modelId),
                prompt = text,
                n = numResponses ?: 1,
                size = size.openAiSize(),
                quality = null,
                style = null
            )
        )
        return images.outputInfo.outputs!!.map { URL(it) }
    }

    private fun ImageSize.openAiSize() = when {
        width == 256 && height == 256 -> com.aallam.openai.api.image.ImageSize.is256x256
        width == 512 && height == 512 -> com.aallam.openai.api.image.ImageSize.is512x512
        width == 1024 && height == 1024 -> com.aallam.openai.api.image.ImageSize.is1024x1024
        else -> throw UnsupportedOperationException()
    }

}
