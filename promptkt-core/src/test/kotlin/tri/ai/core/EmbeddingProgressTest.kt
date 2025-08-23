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