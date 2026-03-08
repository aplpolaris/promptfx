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
package tri.ai.gemini

import tri.ai.core.ImageGenerationParams
import tri.ai.core.ImageGenerator
import tri.ai.gemini.GeminiModelIndex.GEMINI_25_FLASH_IMAGE
import java.net.URI

/** Image generation using Gemini models via the generateContent API with image response modality. */
class GeminiImageGenerator(
    override val modelId: String = GEMINI_25_FLASH_IMAGE,
    override val modelSource: String = GeminiModelIndex.MODEL_SOURCE,
    val client: GeminiClient = GeminiClient.INSTANCE
) : ImageGenerator {

    override fun toString() = modelDisplayName()

    override suspend fun generateImage(text: String, params: ImageGenerationParams): List<URI> {
        val aspectRatio = params.size?.takeIf { ':' in it } ?: "1:1"
        val imageSizeCode = params.size?.takeIf { it.matches(Regex("\\d+K")) } ?: "1K"
        val request = GenerateContentRequest(
            contents = listOf(Content.text(text)),
            generationConfig = GenerationConfig(
                responseModalities = listOf(ResponseModality.IMAGE, ResponseModality.TEXT),
                imageConfig = GeminiImageConfig(
                    aspectRatio = aspectRatio,
                    imageSize = imageSizeCode
                )
            )
        )
        val response = client.generateContent(modelId, request)
        return response.candidates?.flatMap { candidate ->
            candidate.content.parts.mapNotNull { part ->
                part.inlineData?.let { blob ->
                    URI("data:${blob.mimeType};base64,${blob.data}")
                }
            }
        } ?: emptyList()
    }

}
