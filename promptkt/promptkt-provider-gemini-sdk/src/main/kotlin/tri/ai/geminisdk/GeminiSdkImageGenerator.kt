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
package tri.ai.geminisdk

import tri.ai.core.ImageGenerationParams
import tri.ai.core.ImageGenerator
import tri.ai.geminisdk.GeminiSdkModelIndex.GEMINI_25_FLASH_IMAGE
import java.net.URI
import java.util.Base64
import kotlin.jvm.optionals.getOrNull

/**
 * Image generation using the Google GenAI SDK.
 * - For `gemini-*-image` models: uses `generateContent` with `responseModalities = ["IMAGE", "TEXT"]`.
 * - For `imagen-*` models: uses the dedicated `generateImages` API.
 */
class GeminiSdkImageGenerator(
    override val modelId: String = GEMINI_25_FLASH_IMAGE,
    override val modelSource: String = GeminiSdkModelIndex.MODEL_SOURCE,
    val client: GeminiSdkClient = GeminiSdkClient.INSTANCE
) : ImageGenerator {

    override fun toString() = modelDisplayName()

    override suspend fun generateImage(text: String, params: ImageGenerationParams): List<URI> {
        return if (modelId.startsWith("imagen")) {
            generateViaImagenApi(text, params)
        } else {
            generateViaGenerateContent(text, params)
        }
    }

    /** Generate using the Imagen API (for imagen-* models). */
    private fun generateViaImagenApi(text: String, params: ImageGenerationParams): List<URI> {
        val response = client.generateImagesViaImagenApi(
            modelId = modelId,
            prompt = text,
            numberOfImages = params.numResponses ?: 1,
            aspectRatio = params.aspectRatio
        )
        return response.generatedImages().getOrNull()?.mapNotNull { generatedImage ->
            val img = generatedImage.image().getOrNull() ?: return@mapNotNull null
            val bytes = img.imageBytes().getOrNull() ?: return@mapNotNull null
            val mimeType = img.mimeType().getOrNull() ?: "image/png"
            val base64 = Base64.getEncoder().encodeToString(bytes)
            URI("data:$mimeType;base64,$base64")
        } ?: emptyList()
    }

    /** Generate using generateContent with IMAGE response modality (for gemini-*-image models). */
    private fun generateViaGenerateContent(text: String, params: ImageGenerationParams): List<URI> {
        val response = client.generateContentImages(
            modelId = modelId,
            prompt = text,
            aspectRatio = params.aspectRatio,
            imageSize = params.size
        )
        val candidates = response.candidates().getOrNull() ?: return emptyList()
        return candidates.flatMap { candidate ->
            val parts = candidate.content().getOrNull()?.parts()?.getOrNull() ?: return@flatMap emptyList()
            parts.mapNotNull { part ->
                val blob = part.inlineData().getOrNull() ?: return@mapNotNull null
                val bytes = blob.data().getOrNull() ?: return@mapNotNull null
                val mimeType = blob.mimeType().getOrNull() ?: "image/png"
                val base64 = Base64.getEncoder().encodeToString(bytes)
                URI("data:$mimeType;base64,$base64")
            }
        }
    }

}
