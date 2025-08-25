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
package tri.ai.pips.api

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PPlanTest {

    private val mapper = PPlan.MAPPER

    @Test
    fun `parse valid plan with two steps`() {
        val json = """
            {
              "id": "pipelines/demo@1.0.0",
              "steps": [
                {
                  "tool": "util/echo",
                  "input": { "message": "hi" },
                  "saveAs": "a"
                },
                {
                  "tool": "util/merge",
                  "input": { "left": {"${"$"}var": "a"}, "right": {"v": 2} },
                  "onError": "Continue",
                  "timeoutMs": 1500
                }
              ]
            }
        """.trimIndent()

        val plan = PPlan.parse(json)

        assertEquals("pipelines/demo@1.0.0", plan.id)
        assertEquals(2, plan.steps.size)

        val s0 = plan.steps[0]
        assertEquals("util/echo", s0.tool)
        assertEquals("a", s0.saveAs)
        assertEquals(OnError.Fail, s0.onError) // default when omitted
        assertNull(s0.timeoutMs)
        assertEquals("hi", s0.input.get("message").asText())

        val s1 = plan.steps[1]
        assertEquals("util/merge", s1.tool)
        assertEquals(OnError.Continue, s1.onError)
        assertEquals(1500L, s1.timeoutMs)
        assertEquals("a", s1.input.get("left").get("\$var").asText())
        assertEquals(2, s1.input.get("right").get("v").asInt())
    }

    @Test
    fun `defaults are applied and EMPTY is empty`() {
        val json = """
            {
              "steps": [
                { "tool": "only/step", "input": { "x": 1 } }
              ]
            }
        """.trimIndent()

        val plan = PPlan.parse(json)
        assertNull(plan.id)
        assertEquals(1, plan.steps.size)
        val step = plan.steps.first()
        assertEquals(OnError.Fail, step.onError) // default
        assertNull(step.saveAs)
        assertNull(step.timeoutMs)

        // EMPTY constant sanity
        assertNull(PPlan.EMPTY.id)
        assertTrue(PPlan.EMPTY.steps.isEmpty())
    }

    @Test
    fun `invalid plan missing tool should fail to parse`() {
        val badJson = """
            {
              "steps": [
                { "input": { "y": 2 } }
              ]
            }
        """.trimIndent()

        assertThrows<MismatchedInputException> {
            PPlan.parse(badJson)
        }
    }

    @Test
    fun `round-trip via ObjectMapper preserves structure`() {
        val original = PPlan(
            id = "pipelines/rt@0.1.0",
            steps = listOf(
                PPlanStep("util/echo", mapper.readTree("""{"msg":"hello"}"""), saveAs = "m"),
                PPlanStep("util/merge", mapper.readTree("""{"left":{"${"$"}var":"m"}}"""))
            )
        )
        val json = mapper.writeValueAsString(original)
        val reparsed = PPlan.parse(json)

        assertEquals(original.id, reparsed.id)
        assertEquals(original.steps.size, reparsed.steps.size)
        assertEquals("util/echo", reparsed.steps[0].tool)
        assertEquals("hello", reparsed.steps[0].input.get("msg").asText())
        assertEquals("m", reparsed.steps[0].saveAs)
        assertEquals("m", reparsed.steps[1].input.get("left").get("\$var").asText())
    }
}
