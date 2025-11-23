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
import tri.ai.openai.java.OpenAiJavaModelIndex.DALLE3
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Image generation with OpenAI DALL-E models using the official Java SDK. */
class OpenAiJavaImageGenerator(
    override val modelId: String = DALLE3,
    val client: OpenAiJavaClient = OpenAiJavaClient.INSTANCE
) : ImageGenerator {

    override fun toString() = "$modelId (OpenAI Java SDK)"

    override suspend fun generateImage(
        prompt: String,
        n: Int?,
        size: String?,
        quality: String?,
        style: String?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId)
        val t0 = System.currentTimeMillis()

        val paramsBuilder = com.openai.models.images.ImageGenerateParams.builder()
            .model(modelId)
            .prompt(prompt)
            .responseFormat(com.openai.models.images.ImageGenerateParams.ResponseFormat.URL)

        n?.let { paramsBuilder.n(it.toLong()) }
        size?.let { 
            val imageSize = when (size) {
                "256x256" -> com.openai.models.images.ImageGenerateParams.Size._256X256
                "512x512" -> com.openai.models.images.ImageGenerateParams.Size._512X512
                "1024x1024" -> com.openai.models.images.ImageGenerateParams.Size._1024X1024
                "1792x1024" -> com.openai.models.images.ImageGenerateParams.Size._1792X1024
                "1024x1792" -> com.openai.models.images.ImageGenerateParams.Size._1024X1792
                else -> com.openai.models.images.ImageGenerateParams.Size._1024X1024
            }
            paramsBuilder.size(imageSize)
        }
        quality?.let {
            val imageQuality = when (quality.lowercase()) {
                "hd" -> com.openai.models.images.ImageGenerateParams.Quality.HD
                else -> com.openai.models.images.ImageGenerateParams.Quality.STANDARD
            }
            paramsBuilder.quality(imageQuality)
        }
        style?.let {
            val imageStyle = when (style.lowercase()) {
                "natural" -> com.openai.models.images.ImageGenerateParams.Style.NATURAL
                else -> com.openai.models.images.ImageGenerateParams.Style.VIVID
            }
            paramsBuilder.style(imageStyle)
        }

        val response = client.client.images().generate(paramsBuilder.build())

        val imageUrls = response.data().orElse(emptyList()).mapNotNull { it.url().orElse(null) }

        return AiPromptTrace(
            null,
            modelInfo,
            AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
            AiOutputInfo.imageUrls(imageUrls)
        )
    }

}
