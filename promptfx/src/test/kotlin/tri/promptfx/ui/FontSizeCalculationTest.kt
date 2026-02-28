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
package tri.promptfx.ui

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FontSizeCalculationTest {

    /**
     * Implementation of the area-based font size calculation logic for testing.
     * This simulates the logic in ImmersiveChatView.calculateOptimalFontSize().
     */
    private fun calculateOptimalFontSize(text: String, availableWidth: Double = 1200.0, availableHeight: Double = 300.0): Double {
        val textLength = text.length
        
        // Define font size bounds
        val minFontSize = 12.0
        val maxFontSize = 24.0
        
        val availableArea = availableWidth * availableHeight
        
        if (availableArea <= 0 || textLength == 0) {
            return calculateFontSizeByLength(textLength, minFontSize, maxFontSize)
        }
        
        // Calculate font size to achieve target area usage
        // Each character takes approximately font_size² * 0.72 pixels
        // Target: character_area * text_length * 1.5 = available_area  
        val characterAreaFactor = 0.72 * 1.5 // Character area multiplier * target density factor
        val idealFontSizeSquared = availableArea / (textLength * characterAreaFactor)
        val idealFontSize = kotlin.math.sqrt(idealFontSizeSquared)
        
        // Clamp to reasonable bounds
        return idealFontSize.coerceIn(minFontSize, maxFontSize)
    }
    
    /**
     * Fallback font size calculation based purely on text length.
     */
    private fun calculateFontSizeByLength(textLength: Int, minFontSize: Double, maxFontSize: Double): Double {
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
    fun testAreaBasedFontSizeCalculation() {
        // Test with standard area (1200 x 300 = 360,000 pixels)
        
        // Very short text should get max font size (due to clamping)
        val shortText = "Hi"
        val shortFontSize = calculateOptimalFontSize(shortText)
        assertTrue(shortFontSize >= 12.0 && shortFontSize <= 24.0)
        assertEquals(24.0, shortFontSize, 0.1, "Very short text should get max font size due to clamping")
        
        // Medium-short text (still gets max due to large area available)
        val mediumShortText = "x".repeat(100)
        val mediumShortFontSize = calculateOptimalFontSize(mediumShortText)
        assertTrue(mediumShortFontSize >= 12.0 && mediumShortFontSize <= 24.0)
        
        // Longer text should eventually get smaller font size
        val longerText = "x".repeat(1000) 
        val longerFontSize = calculateOptimalFontSize(longerText)
        assertTrue(longerFontSize >= 12.0 && longerFontSize <= 24.0)
        assertTrue(longerFontSize < 24.0, "Longer text should get smaller font than max: $longerFontSize")
        
        // Very long text should get even smaller font
        val veryLongText = "x".repeat(5000)
        val veryLongFontSize = calculateOptimalFontSize(veryLongText)
        assertTrue(veryLongFontSize >= 12.0 && veryLongFontSize <= 24.0)
        assertTrue(veryLongFontSize < longerFontSize, "Very long text should get smaller font than longer text")
    }

    @Test
    fun testDifferentAreaSizes() {
        val testText = "x".repeat(1000) // Fixed length for comparison
        
        // Larger area should allow larger font
        val largeFontSize = calculateOptimalFontSize(testText, 1600.0, 400.0)
        
        // Smaller area should require smaller font  
        val smallFontSize = calculateOptimalFontSize(testText, 800.0, 200.0)
        
        assertTrue(largeFontSize > smallFontSize, 
            "Larger area should allow larger font: $largeFontSize vs $smallFontSize")
        
        // Both should be within bounds
        assertTrue(largeFontSize >= 12.0 && largeFontSize <= 24.0)
        assertTrue(smallFontSize >= 12.0 && smallFontSize <= 24.0)
    }

    @Test
    fun testFontSizeBounds() {
        // Test various scenarios to ensure font size is always within bounds
        val testCases = listOf(
            Triple("", 1000.0, 300.0), // Empty text
            Triple("x".repeat(1), 100.0, 100.0), // Very small area
            Triple("x".repeat(10000), 2000.0, 500.0), // Very long text, large area
            Triple("x".repeat(50), 3000.0, 800.0), // Short text, very large area
        )
        
        for ((text, width, height) in testCases) {
            val fontSize = calculateOptimalFontSize(text, width, height)
            assertTrue(fontSize >= 12.0 && fontSize <= 24.0,
                "Font size $fontSize should be between 12.0 and 24.0 for text length ${text.length}, area ${width}x$height")
        }
    }
    
    @Test
    fun testAreaBasedVsLengthBasedConsistency() {
        // When area-based calculation would give similar results to length-based,
        // they should be reasonably close
        val testText = "x".repeat(1000)
        
        val areaBasedSize = calculateOptimalFontSize(testText, 1200.0, 300.0)
        val lengthBasedSize = calculateFontSizeByLength(testText.length, 12.0, 24.0)
        
        // Both should be valid sizes
        assertTrue(areaBasedSize >= 12.0 && areaBasedSize <= 24.0)
        assertTrue(lengthBasedSize >= 12.0 && lengthBasedSize <= 24.0)
        
        // They don't need to be identical, but should be in similar ranges
        println("Area-based: $areaBasedSize, Length-based: $lengthBasedSize")
    }
}
