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
package tri.ai.gemini

import tri.ai.core.ImageGenerator
import tri.ai.core.ImageSize
import java.net.URL

/** Image generation with Gemini models using Imagen. */
class GeminiImageGenerator(override val modelId: String, val client: GeminiClient = GeminiClient.INSTANCE) :
    ImageGenerator {

    override fun toString() = modelId

    override suspend fun generateImage(text: String, size: ImageSize, prompt: String?, numResponses: Int?): List<URL> {
        try {
            val response = client.generateImage(
                modelId = modelId,
                prompt = text,
                numImages = numResponses ?: 1,
                aspectRatio = size.toAspectRatio()
            )
            
            return response.candidates?.mapNotNull { candidate ->
                // Extract image URLs from the candidate response
                candidate.content.parts.mapNotNull { part ->
                    part.inlineData?.let { blob ->
                        // Create data URL from base64 data
                        URL("data:${blob.mimeType};base64,${blob.data}")
                    }
                }
            }?.flatten() ?: emptyList()
        } catch (e: Exception) {
            // For now, throw a descriptive error until the API is fully supported
            throw UnsupportedOperationException("Gemini image generation is not yet fully implemented. API details need verification. Original error: ${e.message}", e)
        }
    }
    
    private fun ImageSize.toAspectRatio(): String {
        return when {
            width == height -> "1:1"
            width > height -> when {
                width.toDouble() / height > 1.5 -> "16:9"
                else -> "4:3" 
            }
            else -> when {
                height.toDouble() / width > 1.5 -> "9:16"
                else -> "3:4"
            }
        }
    }
}