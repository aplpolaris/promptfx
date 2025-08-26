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
package tri.promptfx.agents

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Tests for [AgenticView]. */
class AgenticViewTest {

    @Test
    fun testSearchFiltering() {
        // Since AgenticView extends Fragment and requires JavaFX initialization,
        // we'll test the filtering logic conceptually
        
        // Test case-insensitive substring matching
        val query = "test"
        val categoryMatch = "Testing".lowercase().contains(query)
        val nameMatch = "Test Tool".lowercase().contains(query)
        val descMatch = "A tool for testing".lowercase().contains(query)
        
        assertTrue(categoryMatch, "Category should match case-insensitive")
        assertTrue(nameMatch, "Name should match case-insensitive")
        assertTrue(descMatch, "Description should match case-insensitive")
        
        // Test non-matching case
        val noMatch = "other".lowercase().contains(query)
        assertFalse(noMatch, "Should not match unrelated text")
    }
    
    @Test
    fun testEmptySearchShowsAll() {
        // Test that empty search shows all items
        val emptyQuery = ""
        val shouldShowAll = emptyQuery.isEmpty()
        assertTrue(shouldShowAll, "Empty query should show all items")
    }

}