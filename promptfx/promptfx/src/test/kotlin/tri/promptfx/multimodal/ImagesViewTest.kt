/*-
 * #%L
 * tri.promptfx:promptfx
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
package tri.promptfx.multimodal

import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.image.Quality
import com.aallam.openai.api.image.Style
import com.aallam.openai.api.model.ModelId
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ImagesViewTest {

    @Test
    fun `test DALL-E 2 model parameters exclude quality and style`() {
        // Simulate the logic from ImagesView.plan() method for DALL-E 2
        val modelValue = "dall-e-2"
        val quality = Quality("standard")
        val style = Style.Companion.Vivid
        
        // This mimics the ImageCreation call in plan() method
        val imageCreation = ImageCreation(
            model = ModelId(modelValue),
            prompt = "test prompt",
            n = 1,
            size = ImageSize.Companion.is512x512,
            quality = if (modelValue == "dall-e-3") quality else null,
            style = if (modelValue == "dall-e-3") style else null
        )
        
        // Verify that quality and style are null for DALL-E 2
        assertNull(imageCreation.quality, "Quality should be null for DALL-E 2")
        assertNull(imageCreation.style, "Style should be null for DALL-E 2")
        assertEquals("dall-e-2", imageCreation.model?.id)
        assertEquals("test prompt", imageCreation.prompt)
        assertEquals(1, imageCreation.n)
        assertEquals(ImageSize.Companion.is512x512, imageCreation.size)
    }

    @Test
    fun `test DALL-E 3 model parameters include quality and style`() {
        // Simulate the logic from ImagesView.plan() method for DALL-E 3
        val modelValue = "dall-e-3"
        val quality = Quality("standard")
        val style = Style.Companion.Vivid
        
        // This mimics the ImageCreation call in plan() method
        val imageCreation = ImageCreation(
            model = ModelId(modelValue),
            prompt = "test prompt",
            n = 1,
            size = ImageSize.Companion.is1024x1024,
            quality = if (modelValue == "dall-e-3") quality else null,
            style = if (modelValue == "dall-e-3") style else null
        )
        
        // Verify that quality and style are included for DALL-E 3
        assertNotNull(imageCreation.quality, "Quality should not be null for DALL-E 3")
        assertNotNull(imageCreation.style, "Style should not be null for DALL-E 3")
        assertEquals(quality, imageCreation.quality)
        assertEquals(style, imageCreation.style)
        assertEquals("dall-e-3", imageCreation.model?.id)
        assertEquals("test prompt", imageCreation.prompt)
        assertEquals(1, imageCreation.n)
        assertEquals(ImageSize.Companion.is1024x1024, imageCreation.size)
    }
}
