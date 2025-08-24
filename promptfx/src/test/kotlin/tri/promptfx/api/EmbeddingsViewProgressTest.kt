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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.EmbeddingModel

class EmbeddingsViewProgressTest {

    /** Test that simulates progress tracking during embedding calculations with multiple inputs. */
    @Test
    fun testEmbeddingsProgressTracking() = runTest {
        // Create a test embedding model that simulates batch processing with progress callbacks
        val progressUpdates = mutableListOf<Pair<String, Double>>()
        
        val mockEmbeddingModel = object : EmbeddingModel {
            override val modelId = "test-embedding-model"
            
            override suspend fun calculateEmbedding(
                text: List<String>,
                outputDimensionality: Int?,
                progressCallback: ((Int, Int) -> Unit)?
            ): List<List<Double>> {
                // Simulate processing text in batches like real implementations do
                val batchSize = 3
                val results = mutableListOf<List<Double>>()
                
                text.chunked(batchSize).forEach { batch ->
                    // Simulate processing each batch
                    Thread.sleep(10) // simulate API call delay
                    batch.forEach { _ -> 
                        results.add(listOf(0.1, 0.2, 0.3, 0.4))
                    }
                    
                    // Report progress after each batch
                    progressCallback?.invoke(results.size, text.size)
                }
                
                return results
            }
        }
        
        // Simulate the progress callback from EmbeddingsView
        val progressCallback: (Int, Int) -> Unit = { completed, total ->
            val message = "Embeddings ($completed/$total)"
            val progressPercent = completed.toDouble() / total.toDouble()
            progressUpdates.add(message to progressPercent)
        }
        
        // Test with multiple text inputs (simulating multi-line input in the UI)
        val testInputs = listOf(
            "First line of text to embed",
            "Second line of text to embed", 
            "Third line of text to embed",
            "Fourth line of text to embed",
            "Fifth line of text to embed",
            "Sixth line of text to embed",
            "Seventh line of text to embed"
        )
        
        // Calculate embeddings with progress tracking
        val results = mockEmbeddingModel.calculateEmbedding(testInputs, null, progressCallback)
        
        // Verify results
        assertEquals(7, results.size)
        results.forEach { embedding ->
            assertEquals(listOf(0.1, 0.2, 0.3, 0.4), embedding)
        }
        
        // Verify progress updates were called
        assertTrue(progressUpdates.isNotEmpty(), "Progress updates should have been called")
        
        // Progress should show completion status
        val finalUpdate = progressUpdates.last()
        assertEquals("Embeddings (7/7)", finalUpdate.first)
        assertEquals(1.0, finalUpdate.second, 0.001)
        
        // Should have intermediate progress updates due to batching
        assertTrue(progressUpdates.size >= 2, "Should have multiple progress updates due to batching")
        
        // All progress percentages should be between 0 and 1
        progressUpdates.forEach { (_, percent) ->
            assertTrue(percent >= 0.0 && percent <= 1.0, "Progress percent should be between 0.0 and 1.0, got: $percent")
        }
        
        println("Progress updates received:")
        progressUpdates.forEach { (message, percent) ->
            println("  $message (${(percent * 100).toInt()}%)")
        }
    }
    
    /** Test that embedding calculation works correctly without progress callback (backward compatibility). */
    @Test
    fun testBackwardCompatibility() = runTest {
        val mockEmbeddingModel = object : EmbeddingModel {
            override val modelId = "test-model"
            
            override suspend fun calculateEmbedding(
                text: List<String>,
                outputDimensionality: Int?,
                progressCallback: ((Int, Int) -> Unit)?
            ): List<List<Double>> {
                // Should handle null progress callback gracefully
                progressCallback?.invoke(text.size, text.size)
                return text.map { listOf(0.5, 0.6, 0.7) }
            }
        }
        
        // Test without progress callback (backward compatibility)
        val testInputs = listOf("test1", "test2", "test3")
        val results = mockEmbeddingModel.calculateEmbedding(testInputs, null, null)
        
        assertEquals(3, results.size)
        results.forEach { embedding ->
            assertEquals(listOf(0.5, 0.6, 0.7), embedding)
        }
    }
}