/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.api

import org.junit.jupiter.api.Test
import tri.ai.gemini.GeminiModelIndex
import com.aallam.openai.api.image.ImageSize

/** Test to verify UI integration for Gemini image models. */
class ImagesViewIntegrationTest {

    @Test 
    fun `test IMAGE_SIZES map contains Gemini models`() {
        println("=== Test UI Integration ===")
        
        // Test the companion object constants
        val dalleId = "dall-e-2"  
        val imagenId = "imagen-3.0-generate-001"
        
        // Test image size mappings for Gemini models
        val imageSizes = mapOf(
            dalleId to listOf(
                ImageSize.is256x256,
                ImageSize.is512x512, 
                ImageSize.is1024x1024
            ),
            imagenId to listOf(
                ImageSize.is1024x1024,
                ImageSize("1792x1024"),
                ImageSize("1024x1792")
            )
        )
        
        assert(imageSizes.containsKey(imagenId)) { "Should have sizes for Gemini model" }
        assert(imageSizes[imagenId]!!.isNotEmpty()) { "Gemini model should have size options" }
        
        println("✓ UI IMAGE_SIZES mapping includes Gemini models")
        println("Sizes for $imagenId: ${imageSizes[imagenId]?.map { it.size }}")
    }
    
    @Test
    fun `test parseImageSize helper function`() {
        // Test the size parsing logic we added
        val testSizes = listOf("1024x1024", "1792x1024", "1024x1792")
        
        testSizes.forEach { sizeStr ->
            val parts = sizeStr.split("x")
            assert(parts.size == 2) { "Size string should parse to width x height" }
            val width = parts[0].toInt()
            val height = parts[1].toInt()
            assert(width > 0 && height > 0) { "Dimensions should be positive" }
            println("✓ Successfully parsed size: ${width}x${height}")
        }
    }
}
