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

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
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
    @Test
    fun testTemplateMetadataCompatibility() {
        val prompt = AiPromptLibrary.INSTANCE.prompts["question-answer"]
        Assertions.assertNotNull(prompt!!.templateDescription)
        Assertions.assertNotNull(prompt.templateName)
        Assertions.assertTrue(prompt.template.isNotEmpty())
    }

    @Test
    fun testParsePrompt() {
        val promptA = "this is a prompt"
        val promptB = """
          template-name:
            Default Model Creation
          template-description:
            Use this tool to create a model
          prompt-template: >
            I need you to generate json as output.
            '''
            OUTPUT=[
              "A", "B", "C"
            ]
            '''
            INPUT={{input}}
            OUTPUT=
        """.trimIndent()

        val parsedPromptA = AiPromptLibrary.MAPPER.readValue<AiPrompt>(promptA)
        Assertions.assertTrue(parsedPromptA.templateName.isEmpty())
        Assertions.assertTrue(parsedPromptA.templateDescription.isEmpty())
        Assertions.assertTrue(parsedPromptA.template.isNotEmpty())

        val parsedPromptB = AiPromptLibrary.MAPPER.readValue<AiPrompt>(promptB)
        Assertions.assertTrue(parsedPromptB.templateName.isNotEmpty())
        Assertions.assertTrue(parsedPromptB.templateDescription.isNotEmpty())
        Assertions.assertTrue(parsedPromptB.template.isNotEmpty())
    }

    @Test
    fun testPromptWrite() {
        val prompt = AiPrompt("a template", "description", "name")
        assertEquals("""
            ---
            prompt-template: "a template"
            template-description: "description"
            template-name: "name"
            
        """.trimIndent(), AiPromptLibrary.MAPPER.writeValueAsString(prompt))
    }
}
