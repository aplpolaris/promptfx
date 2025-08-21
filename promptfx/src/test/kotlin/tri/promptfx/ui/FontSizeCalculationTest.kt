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
package tri.promptfx.ui

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FontSizeCalculationTest {

    /**
     * Implementation of the font size calculation logic for testing.
     * This matches the logic in ImmersiveChatView.calculateOptimalFontSize().
     */
    private fun calculateOptimalFontSize(text: String): Double {
        val textLength = text.length
        
        // Define font size bounds
        val minFontSize = 12.0
        val maxFontSize = 24.0
        
        // Define text length thresholds
        val shortTextThreshold = 100    // Very short responses get max font size
        val longTextThreshold = 2000    // Very long responses get min font size
        
        return when {
            textLength <= shortTextThreshold -> maxFontSize
            textLength >= longTextThreshold -> minFontSize
            else -> {
                // Linear interpolation between max and min font size
                val ratio = (textLength - shortTextThreshold).toDouble() / (longTextThreshold - shortTextThreshold)
                maxFontSize - (ratio * (maxFontSize - minFontSize))
            }
        }
    }

    @Test
    fun testCalculateOptimalFontSize() {
        // Test very short text - should get max font size (24.0)
        val shortText = "Hi"
        assertEquals(24.0, calculateOptimalFontSize(shortText), 0.1)
        
        // Test text at short threshold (100 chars) - should get max font size
        val shortThresholdText = "x".repeat(100)
        assertEquals(24.0, calculateOptimalFontSize(shortThresholdText), 0.1)
        
        // Test medium text (500 chars) - should get interpolated size
        val mediumText = "x".repeat(500)
        val mediumFontSize = calculateOptimalFontSize(mediumText)
        assertTrue(mediumFontSize > 12.0 && mediumFontSize < 24.0)
        
        // Test long text (1000 chars) - should get smaller interpolated size
        val longText = "x".repeat(1000)  
        val longFontSize = calculateOptimalFontSize(longText)
        assertTrue(longFontSize > 12.0 && longFontSize < mediumFontSize)
        
        // Test text at long threshold (2000 chars) - should get min font size (12.0)
        val longThresholdText = "x".repeat(2000)
        assertEquals(12.0, calculateOptimalFontSize(longThresholdText), 0.1)
        
        // Test very long text - should get min font size (12.0)  
        val veryLongText = "x".repeat(5000)
        assertEquals(12.0, calculateOptimalFontSize(veryLongText), 0.1)
    }

    @Test
    fun testFontSizeDecreaseWithLength() {
        // Test that font size decreases as text length increases
        val sizes = listOf(100, 500, 1000, 1500, 2000).map { length ->
            val text = "x".repeat(length)
            calculateOptimalFontSize(text)
        }
        
        // Check that each font size is less than or equal to the previous one
        for (i in 1 until sizes.size) {
            assertTrue(sizes[i] <= sizes[i-1], 
                "Font size should decrease as text length increases: ${sizes[i-1]} -> ${sizes[i]}")
        }
    }

    @Test
    fun testFontSizeBounds() {
        // Test various text lengths to ensure font size is always within bounds
        val testLengths = listOf(0, 50, 100, 150, 500, 1000, 1500, 2000, 3000, 5000)
        
        for (length in testLengths) {
            val text = "x".repeat(length)
            val fontSize = calculateOptimalFontSize(text)
            assertTrue(fontSize >= 12.0 && fontSize <= 24.0,
                "Font size $fontSize should be between 12.0 and 24.0 for text length $length")
        }
    }
}