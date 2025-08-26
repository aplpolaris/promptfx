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
package tri.util.io.pdf

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.Weigher
import tri.util.fine
import tri.util.info
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages caching of images extracted from PDF documents to improve performance
 * and prevent memory issues with large document sets.
 */
object PdfImageCache {

    // Default cache memory limit (500MB)
    private const val DEFAULT_MEMORY_LIMIT_MB = 500L

    // Cache key for a specific image from a PDF
    data class ImageCacheKey(
        val pdfPath: String,
        val pageIndex: Int,
        val imageIndex: Int
    )

    // Estimate memory weight of a BufferedImage (width * height * 4 bytes per pixel for RGBA)
    private val imageWeigher = Weigher<ImageCacheKey, BufferedImage> { _, image ->
        image.width * image.height * 4
    }

    // Cache with configurable memory limit and 1 hour expiration
    private val imageCache: Cache<ImageCacheKey, BufferedImage> = CacheBuilder.newBuilder()
        .maximumWeight(DEFAULT_MEMORY_LIMIT_MB * 1024 * 1024L) // Convert MB to bytes
        .weigher(imageWeigher)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .removalListener<ImageCacheKey, BufferedImage> { notification ->
            if (notification.wasEvicted()) {
                fine<PdfImageCache>("Evicted image from cache: ${notification.key?.pdfPath} (${notification.cause})")
            }
        }
        .build()

    /**
     * Extract all images from a PDF file, using cache when possible.
     * Returns a list of BufferedImage objects that are cached for future use.
     */
    fun getImagesFromPdf(pdfFile: File): List<BufferedImage> {
        if (!pdfFile.exists() || pdfFile.extension.lowercase() != "pdf") {
            return emptyList()
        }

        val pdfPath = pdfFile.absolutePath
        val result = mutableListOf<BufferedImage>()

        try {
            val pageInfoList = PdfUtils.pdfPageInfo(pdfFile, findImages = true)

            pageInfoList.forEachIndexed { pageIndex, pageInfo ->
                pageInfo.images.forEachIndexed { imageIndex, imageInfo ->
                    val key = ImageCacheKey(pdfPath, pageIndex, imageIndex)

                    val cachedImage = imageCache.getIfPresent(key)
                    if (cachedImage != null) {
                        result.add(cachedImage)
                    } else {
                        imageInfo.image?.let { bufferedImage ->
                            // Store in cache for future use
                            imageCache.put(key, bufferedImage)
                            result.add(bufferedImage)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't break the application
            info<PdfImageCache>("Error extracting images from PDF ${pdfFile.name}: ${e.message}")
        }

        info<PdfImageCache>("Loaded ${result.size} images from ${pdfFile.name} (${imageCache.size()} total cached)")
        return result.distinctBy { it.hashCode() } // Remove duplicates
    }

    /** Remove cached images for a specific PDF file. */
    fun evictPdf(pdfFile: File) {
        val pdfPath = pdfFile.absolutePath
        imageCache.asMap().keys.removeIf { it.pdfPath == pdfPath }
    }

    /** Get cache statistics for monitoring and debugging. */
    fun getCacheStats() = imageCache.stats()
    /** Clear the entire cache. Useful for testing or when memory is needed. */
    fun clearCache() = imageCache.invalidateAll()
    /** Get current cache size (number of entries). */
    fun getCacheSize() = imageCache.size()
    /** Get estimated memory usage of cached images in bytes. */
    fun getEstimatedMemoryUsage(): Long = imageCache.asMap().values.sumOf { image ->
        imageWeigher.weigh(null, image).toLong()
    }
}