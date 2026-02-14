/*-
 * #%L
 * tri.promptfx:promptfx-sample-plugin
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
package tri.promptfx.sample

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import tri.util.ui.NavigableWorkspaceView
import tri.util.ui.WorkspaceViewAffordance
import java.util.*

class SamplePluginTest {

    @Test
    fun `plugin should be discoverable via ServiceLoader`() {
        val loader = ServiceLoader.load(NavigableWorkspaceView::class.java)
        val plugins = loader.toList()
        
        val samplePlugin = plugins.find { it is SamplePlugin }
        assertNotNull(samplePlugin, "SamplePlugin should be discoverable via ServiceLoader")
    }

    @Test
    fun `plugin should have correct properties`() {
        val plugin = SamplePlugin()
        
        assertEquals("Sample Plugin", plugin.category)
        assertEquals("Hello World", plugin.name)
        assertEquals(WorkspaceViewAffordance.INPUT_ONLY, plugin.affordances)
    }

}
