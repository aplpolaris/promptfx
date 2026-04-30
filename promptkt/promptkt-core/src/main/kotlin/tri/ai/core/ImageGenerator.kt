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
package tri.ai.core

import java.net.URI

/** Interface for image generation. */
interface ImageGenerator : AiModel {
    /** Generate images, returning a URI for each (may be an HTTP URL or a data: URI for base64-encoded images). */
    suspend fun generateImage(
        text: String,
        params: ImageGenerationParams = ImageGenerationParams()
    ): List<URI>
}

/** Parameters for image generation. Each generator uses the fields relevant to its API. */
data class ImageGenerationParams(
    /** Size string: pixel dimensions (e.g. "1024x1024") for OpenAI, image size code (e.g. "1K") for Gemini. */
    val size: String? = null,
    /** Aspect ratio string (e.g. "1:1", "16:9"). Used by Gemini; ignored by OpenAI which uses [size] for dimensions. */
    val aspectRatio: String? = null,
    /** Number of images to generate. */
    val numResponses: Int? = null,
    /** Quality setting (model-specific, e.g. "standard", "hd", "high", "auto"). */
    val quality: String? = null,
    /** Style setting (model-specific, e.g. "vivid", "natural"). */
    val style: String? = null
)

data class ImageSize(val width: Int, val height: Int)
