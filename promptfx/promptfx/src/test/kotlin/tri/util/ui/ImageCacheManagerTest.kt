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
package tri.util.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ImageCacheManagerTest {

    @BeforeEach
    fun setUp() {
        ImageCacheManager.clearCache()
    }

    @Test
    fun testCacheInitiallyEmpty() {
        val stats = ImageCacheManager.getCacheStats()
        assertEquals(0L, stats.requestCount())
    }

    @Test
    fun testGetImagesFromNonexistentFile() {
        val nonExistentFile = File("nonexistent.pdf")
        val images = ImageCacheManager.getImagesFromPdf(nonExistentFile)
        assertEquals(0, images.size)
    }

    @Test
    fun testGetImagesFromNonPdfFile() {
        val textFile = File.createTempFile("test", ".txt")
        textFile.writeText("This is not a PDF file")
        try {
            val images = ImageCacheManager.getImagesFromPdf(textFile)
            assertEquals(0, images.size)
        } finally {
            textFile.delete()
        }
    }

    @Test
    fun testClearCache() {
        // Cache should be empty initially
        val initialStats = ImageCacheManager.getCacheStats()
        assertEquals(0L, initialStats.requestCount())

        // Clear cache shouldn't cause issues
        ImageCacheManager.clearCache()

        // Cache should still be empty after clearing
        val afterClearStats = ImageCacheManager.getCacheStats()
        assertEquals(0L, afterClearStats.requestCount())
    }
}
