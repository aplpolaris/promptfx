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
package tri.ai.prompt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PromptTemplateTest {

    val TEMPLATE_2 = PromptTemplate("Turn the color {{input}} into a hex code.")
    val TEMPLATE_3 = PromptTemplate("Today is {{today}}")

    @Test
    fun testPromptFillInput() {
        val result2 = TEMPLATE_2.fillInput("red")
        println(result2)
        Assertions.assertEquals("Turn the color red into a hex code.", result2)
    }

    @Test
    fun testPromptFillToday() {
        val result3 = TEMPLATE_3.fill()
        println(result3)
        Assertions.assertFalse(result3.contains("{{today}}"))
    }

}
