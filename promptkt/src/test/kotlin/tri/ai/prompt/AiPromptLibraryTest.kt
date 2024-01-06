/*-
 * #%L
 * promptkt-0.1.0-SNAPSHOT
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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

class AiPromptLibraryTest {

    @Test
    fun testPromptExists() {
        val prompt = AiPromptLibrary.INSTANCE.prompts["question-answer"]
        Assertions.assertNotNull(prompt)
    }

    @Test
    fun testPromptFill() {
        val prompt = AiPromptLibrary.INSTANCE.prompts["question-answer"]
        val result = prompt!!.instruct(instruct = "What is the meaning of life?",
            input = "42")
        println(result)
        Assertions.assertEquals(161, result.length)
    }

}
