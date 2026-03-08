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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.ImageGenerationParams

@Tag("openai")
class OpenAiImageGeneratorTest {

    private val dalleGenerator = OpenAiImageGenerator(OpenAiModelIndex.IMAGE_DALLE2)

    @Test
    @Tag("openai")
    fun testGenerateImage_Simple() = runTest {
        val uris = dalleGenerator.generateImage(
            text = "A simple red circle on a white background",
            params = ImageGenerationParams(size = "1024x1024")
        )
        println("Generated ${uris.size} image(s)")
        uris.forEach { println("  URI scheme: ${it.scheme}, length: ${it.toString().length}") }

        assertTrue(uris.isNotEmpty(), "Expected at least one image URI")
        val uri = uris.first()
        assertEquals("data", uri.scheme, "Expected a data: URI")
        assertTrue(uri.toString().contains(";base64,"), "Expected base64-encoded image data")
    }

    @Test
    @Tag("openai")
    fun testGenerateImage_ReturnsDataUri() = runTest {
        val uris = dalleGenerator.generateImage(
            text = "A blue triangle",
            params = ImageGenerationParams(size = "1024x1024")
        )

        assertTrue(uris.isNotEmpty(), "Expected at least one image URI")
        for (uri in uris) {
            val uriStr = uri.toString()
            assertTrue(uriStr.startsWith("data:image/"), "URI should be an image data URI: ${uriStr.take(30)}")
            assertTrue(uriStr.contains(";base64,"), "URI should contain base64 data")
            val base64Part = uriStr.substringAfter(";base64,")
            assertTrue(base64Part.isNotEmpty(), "Base64 data should not be empty")
        }
    }

    @Test
    @Tag("openai")
    fun testGenerateMultipleImages() = runTest {
        val uris = dalleGenerator.generateImage(
            text = "A small green square",
            params = ImageGenerationParams(size = "256x256", numResponses = 2)
        )
        println("Generated ${uris.size} image(s)")
        assertEquals(2, uris.size, "Expected 2 images")
        uris.forEach { uri ->
            assertTrue(uri.toString().startsWith("data:"), "Each URI should be a data: URI")
        }
    }

    @Test
    @Tag("openai")
    fun testModelId() {
        assertEquals(OpenAiModelIndex.IMAGE_DALLE2, dalleGenerator.modelId)
        assertEquals(OpenAiModelIndex.MODEL_SOURCE, dalleGenerator.modelSource)
    }

    @Test
    @Tag("openai")
    fun testGenerateImage_DallE3() = runTest {
        val generator = OpenAiImageGenerator("dall-e-3")
        val uris = generator.generateImage(
            text = "A photorealistic golden sunset over the ocean",
            params = ImageGenerationParams(size = "1024x1024", quality = "standard", style = "vivid")
        )
        println("DALL-E 3 generated ${uris.size} image(s)")
        assertTrue(uris.isNotEmpty(), "Expected at least one image from DALL-E 3")
        assertTrue(uris.first().toString().startsWith("data:"), "Expected data: URI")
    }

}
