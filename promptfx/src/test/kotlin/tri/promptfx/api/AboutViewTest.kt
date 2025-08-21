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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AboutViewTest {

    @Test
    fun testGetApplicationVersion() {
        val version = AboutView.getApplicationVersion()
        
        // Version should not be null or empty
        assertNotNull(version)
        assertTrue(version.isNotEmpty())
        
        // Should contain reasonable version format (numbers and possibly -SNAPSHOT)
        assertTrue(version.matches(Regex("\\d+\\.\\d+.*")), 
                  "Version should start with major.minor format, but was: $version")
        
        println("Detected application version: $version")
    }
}