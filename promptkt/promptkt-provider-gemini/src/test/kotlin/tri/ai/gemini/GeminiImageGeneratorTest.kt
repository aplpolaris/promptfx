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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tri.ai.core.ImageGenerationParams
import tri.ai.gemini.GeminiModelIndex.GEMINI_25_FLASH_IMAGE

@Tag("gemini")
class GeminiImageGeneratorTest {

    private val generator = GeminiImageGenerator(GEMINI_25_FLASH_IMAGE)

    @Test
    @Tag("gemini")
    fun testGenerateImage_Simple() = runTest {
        val uris = generator.generateImage(
            text = "A simple red circle on a white background",
            params = ImageGenerationParams(size = "1:1")
        )
        println("Generated ${uris.size} image(s)")
        uris.forEach { println("  URI scheme: ${it.scheme}, length: ${it.toString().length}") }

        assertTrue(uris.isNotEmpty(), "Expected at least one image URI")
        val uri = uris.first()
        assertEquals("data", uri.scheme, "Expected a data: URI")
        assertTrue(uri.toString().contains(";base64,"), "Expected base64-encoded image data")
    }

    @Test
    @Tag("gemini")
    fun testGenerateImage_ReturnsDataUri() = runTest {
        val uris = generator.generateImage(
            text = "A blue triangle",
            params = ImageGenerationParams(size = "1:1")
        )

        assertTrue(uris.isNotEmpty(), "Expected at least one image URI")
        for (uri in uris) {
            val uriStr = uri.toString()
            assertTrue(uriStr.startsWith("data:"), "URI should start with 'data:': $uriStr")
            assertTrue(uriStr.contains(";base64,"), "URI should contain base64 data: $uriStr")
            val base64Part = uriStr.substringAfter(";base64,")
            assertTrue(base64Part.isNotEmpty(), "Base64 data should not be empty")
        }
    }

    @Test
    @Tag("gemini")
    fun testGenerateImage_MimeType() = runTest {
        val uris = generator.generateImage(
            text = "A green star",
            params = ImageGenerationParams(size = "1:1")
        )

        assertTrue(uris.isNotEmpty(), "Expected at least one image URI")
        val uri = uris.first()
        val uriStr = uri.toString()
        // Gemini image generation returns image/png or image/jpeg
        assertTrue(
            uriStr.startsWith("data:image/"),
            "URI should have an image MIME type, got: ${uriStr.take(30)}"
        )
    }

    @Test
    @Tag("gemini")
    fun testGenerateImage_WideAspectRatio() = runTest {
        val uris = generator.generateImage(
            text = "A wide panoramic mountain landscape",
            params = ImageGenerationParams(size = "16:9")
        )

        assertTrue(uris.isNotEmpty(), "Expected at least one image URI")
        println("16:9 image URI length: ${uris.first().toString().length}")
    }

    @Test
    @Tag("gemini")
    fun testModelId() {
        assertEquals(GEMINI_25_FLASH_IMAGE, generator.modelId)
        assertEquals(GeminiModelIndex.MODEL_SOURCE, generator.modelSource)
    }

}
