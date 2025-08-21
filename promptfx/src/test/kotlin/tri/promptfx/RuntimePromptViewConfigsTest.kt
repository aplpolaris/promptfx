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
package tri.promptfx

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuntimePromptViewConfigsTest {

    @Test
    fun testLoadViews() {
        val views = RuntimePromptViewConfigs.views
        views.values.groupBy { it.category }.toSortedMap().forEach { (category, viewList) ->
            println("$category/")
            viewList.sortedBy { it.title }.forEach {
                println("  " + it.title.padEnd(30) + it.promptConfig().id)
            }
        }
        assertTrue(views.isNotEmpty())
    }

    @Test
    fun testLoadMcpViews() {
        val views = RuntimePromptViewConfigs.mcpViews
        views.values.groupBy { it.category }.toSortedMap().forEach { (category, viewList) ->
            println("$category/")
            viewList.sortedBy { it.prompt.title!! }.forEach {
                println("  " + it.prompt.title!!.padEnd(30) + it.prompt.id)
            }
        }
        assertTrue(views.isNotEmpty())
    }

}
