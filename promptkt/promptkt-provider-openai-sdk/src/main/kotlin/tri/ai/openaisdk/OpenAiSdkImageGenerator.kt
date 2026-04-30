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
package tri.ai.openaisdk

import com.openai.models.images.ImageGenerateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tri.ai.core.ImageGenerationParams
import tri.ai.core.ImageGenerator
import java.net.URI

/** Image generation using the OpenAI official Java SDK. */
class OpenAiSdkImageGenerator(
    override val modelId: String = OpenAiSdkModelIndex.IMAGE_DALLE2,
    override val modelSource: String = OpenAiSdkModelIndex.MODEL_SOURCE,
    val client: OpenAiSdkClient = OpenAiSdkClient.INSTANCE
) : ImageGenerator {

    override fun toString() = modelDisplayName()

    override suspend fun generateImage(text: String, params: ImageGenerationParams): List<URI> =
        withContext(Dispatchers.IO) {
            val paramsBuilder = ImageGenerateParams.builder()
                .model(modelId)
                .prompt(text)
                .n((params.numResponses ?: 1).toLong())
                .responseFormat(ImageGenerateParams.ResponseFormat.B64_JSON)
            params.size?.let { paramsBuilder.size(ImageGenerateParams.Size.of(it)) }
            params.quality?.let { paramsBuilder.quality(ImageGenerateParams.Quality.of(it)) }
            params.style?.let { paramsBuilder.style(ImageGenerateParams.Style.of(it)) }

            val response = client.getClient().images().generate(paramsBuilder.build())
            val images = response.data().orElse(emptyList())
            images.map { image ->
                val b64 = image.b64Json().orElseThrow {
                    IllegalStateException("No base64 image content in response for model $modelId")
                }
                URI("data:image/png;base64,$b64")
            }
        }

}
