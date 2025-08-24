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
package tri.ai.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class EmbeddingProgressTest {

    /** Test that progress callbacks are invoked correctly during embedding calculations. */
    @Test
    fun testProgressCallback() = runTest {
        // Create a mock embedding model that tracks progress calls
        val progressCalls = mutableListOf<Pair<Int, Int>>()
        val progressCallback: (Int, Int) -> Unit = { completed, total ->
            progressCalls.add(completed to total)
        }
        
        val testModel = object : EmbeddingModel {
            override val modelId = "test-model"
            
            override suspend fun calculateEmbedding(
                text: List<String>,
                outputDimensionality: Int?,
                progressCallback: ((Int, Int) -> Unit)?
            ): List<List<Double>> {
                // Simulate progress reporting during embedding calculation
                text.forEachIndexed { index, _ ->
                    progressCallback?.invoke(index + 1, text.size)
                }
                return text.map { listOf(0.1, 0.2, 0.3) }
            }
        }
        
        // Test with multiple inputs
        val inputs = listOf("text1", "text2", "text3", "text4")
        val results = testModel.calculateEmbedding(inputs, null, progressCallback)
        
        // Verify results
        assertEquals(4, results.size)
        assertEquals(listOf(0.1, 0.2, 0.3), results[0])
        
        // Verify progress calls
        assertEquals(4, progressCalls.size)
        assertEquals(1 to 4, progressCalls[0])
        assertEquals(2 to 4, progressCalls[1])
        assertEquals(3 to 4, progressCalls[2])
        assertEquals(4 to 4, progressCalls[3])
    }

    /** Test that progress callbacks work correctly with caching scenarios. */
    @Test
    fun testProgressWithCaching() = runTest {
        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val progressCallback: (Int, Int) -> Unit = { completed, total ->
            progressUpdates.add(completed to total)
        }
        
        val testModel = object : EmbeddingModel {
            override val modelId = "test-model-with-cache"
            private val cache = mutableMapOf<String, List<Double>>()
            
            override suspend fun calculateEmbedding(
                text: List<String>,
                outputDimensionality: Int?,
                progressCallback: ((Int, Int) -> Unit)?
            ): List<List<Double>> {
                // Simulate caching logic like real implementations
                val uncached = text.filter { it !in cache }
                var processedCount = 0
                
                // Process uncached items in batches
                uncached.chunked(2).forEach { batch ->
                    Thread.sleep(5) // simulate processing time
                    batch.forEach { item ->
                        cache[item] = listOf(0.1, 0.2, 0.3)
                    }
                    processedCount += batch.size
                    // Report progress: cached items + newly processed items
                    progressCallback?.invoke(text.size - uncached.size + processedCount, text.size)
                }
                
                return text.map { cache[it]!! }
            }
        }
        
        // First call - all items need processing
        val texts1 = listOf("A", "B", "C", "D")
        val result1 = testModel.calculateEmbedding(texts1, null, progressCallback)
        assertEquals(4, result1.size)
        
        // Should have progress updates
        assertTrue(progressUpdates.isNotEmpty())
        assertEquals(4 to 4, progressUpdates.last()) // All completed
        
        progressUpdates.clear()
        
        // Second call - some cached, some new
        val texts2 = listOf("A", "E", "B", "F") // A and B are cached
        val result2 = testModel.calculateEmbedding(texts2, null, progressCallback)
        assertEquals(4, result2.size)
        
        // Should report progress correctly with cached items
        assertTrue(progressUpdates.isNotEmpty())
        assertEquals(4 to 4, progressUpdates.last()) // All 4 completed (2 from cache + 2 processed)
        
        // First progress update should account for immediately available cached items
        if (progressUpdates.size >= 2) {
            assertTrue(progressUpdates[0].first >= 2, "Should account for cached items: ${progressUpdates[0]}")
        }
        
        println("Caching scenario progress updates:")
        progressUpdates.forEach { (completed, total) ->
            println("  $completed/$total (${(completed.toDouble() / total * 100).toInt()}%)")
        }
    }

    /** Test that embedding models work correctly without progress callbacks. */
    @Test
    fun testWithoutProgressCallback() = runTest {
        val testModel = object : EmbeddingModel {
            override val modelId = "test-model"
            
            override suspend fun calculateEmbedding(
                text: List<String>,
                outputDimensionality: Int?,
                progressCallback: ((Int, Int) -> Unit)?
            ): List<List<Double>> {
                // Should work fine even when progressCallback is null
                progressCallback?.invoke(text.size, text.size)
                return text.map { listOf(0.5) }
            }
        }
        
        // Test without progress callback (should not throw)
        val results = testModel.calculateEmbedding(listOf("test"), null, null)
        assertEquals(1, results.size)
        assertEquals(listOf(0.5), results[0])
        
        // Test using the convenience method (should not throw)
        val singleResult = testModel.calculateEmbedding("test")
        assertEquals(listOf(0.5), singleResult)
    }
}