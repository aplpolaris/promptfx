/*-
 * #%L
 * tri.promptfx:promptkt
 * %%
 * Copyright (C) 2023 - 2025 Johns Hopkins University Applied Physics Laboratory
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
package tri.ai.openai.java

import tri.ai.core.ImageGenerator
import tri.ai.core.ImageSize
import java.net.URL

/** Image generation with OpenAI DALL-E models using the official Java SDK. */
class OpenAiJavaImageGenerator(
    override val modelId: String = OpenAiJavaModelIndex.DALLE3,
    val client: OpenAiJavaClient = OpenAiJavaClient.INSTANCE
) : ImageGenerator {

    override fun toString() = "$modelId (OpenAI Java SDK)"

    override suspend fun generateImage(
        text: String,
        size: ImageSize,
        prompt: String?,
        numResponses: Int?
    ): List<URL> {
        val modelInfo = AiModelInfo.info(modelId)
        val t0 = System.currentTimeMillis()

        val paramsBuilder = com.openai.models.images.ImageGenerateParams.builder()
            .model(modelId)
            .prompt(prompt ?: text)
            .responseFormat(com.openai.models.images.ImageGenerateParams.ResponseFormat.URL)

        numResponses?.let { paramsBuilder.n(it.toLong()) }
        
        val imageSize = when (size) {
            ImageSize.SMALL -> com.openai.models.images.ImageGenerateParams.Size._256X256
            ImageSize.MEDIUM -> com.openai.models.images.ImageGenerateParams.Size._512X512
            ImageSize.LARGE -> com.openai.models.images.ImageGenerateParams.Size._1024X1024
        }
        paramsBuilder.size(imageSize)

        val response = client.client.images().generate(paramsBuilder.build())

        return response.data().orElse(emptyList()).mapNotNull { 
            it.url().orElse(null)?.let { urlStr -> URL(urlStr) }
        }
    }

}
