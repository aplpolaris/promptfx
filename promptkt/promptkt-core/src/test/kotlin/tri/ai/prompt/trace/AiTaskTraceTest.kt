/*-
 * #%L
 * tri.promptfx:promptkt
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
package tri.ai.prompt.trace

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.util.json.jsonWriter

class AiTaskTraceTest {

    //region BASIC CONSTRUCTION

    @Test
    fun testDefaultConstruction() {
        val trace = AiTaskTrace()
        assertNotNull(trace.taskId)
        assertNull(trace.parentTaskId)
        assertNull(trace.callerId)
        assertNull(trace.env)
        assertNull(trace.input)
        assertNull(trace.output)
        assertTrue(trace.exec.succeeded())
    }

    @Test
    fun testPrimaryConstructorWithNewFields() {
        val trace = AiTaskTrace(
            taskId = "task-1",
            parentTaskId = "parent-0",
            callerId = "my-view",
            env = AiEnvInfo(model = AiModelInfo("gpt-4o")),
            input = AiTaskInputInfo(prompt = "Tell me about {{topic}}", params = mapOf("topic" to "AI")),
            exec = AiExecInfo(),
            output = AiOutputInfo.text("AI stands for artificial intelligence.")
        )
        assertEquals("task-1", trace.taskId)
        assertEquals("parent-0", trace.parentTaskId)
        assertEquals("my-view", trace.callerId)
        assertEquals("gpt-4o", trace.env?.modelId)
        assertEquals("Tell me about {{topic}}", trace.input?.prompt)
        assertEquals("AI", trace.input?.params?.get("topic"))
        assertEquals("AI stands for artificial intelligence.", trace.firstValue.textContent())
    }

    //endregion

    //region BACKWARD COMPAT CONSTRUCTOR

    @Test
    fun testBackwardCompatConstructorWithPromptInfo() {
        val promptInfo = PromptInfo("Translate {{text}} to French.", mapOf("text" to "Hello!"))
        val modelInfo = AiModelInfo("test-model")
        val execInfo = AiExecInfo()
        val outputInfo = AiOutputInfo.text("Bonjour!")

        val trace = AiTaskTrace(promptInfo, modelInfo, execInfo, outputInfo)

        // New-style access
        assertEquals("test-model", trace.env?.modelId)
        assertEquals("Translate {{text}} to French.", trace.input?.prompt)
        assertEquals("Hello!", trace.input?.params?.get("text"))
        assertEquals("Bonjour!", trace.firstValue.textContent())

        // Backward compat property
        @Suppress("DEPRECATION")
        assertEquals("Translate {{text}} to French.", trace.prompt?.template)
        @Suppress("DEPRECATION")
        assertEquals("Hello!", trace.prompt?.params?.get("text"))
    }

    @Test
    fun testBackwardCompatConstructorNullPromptInfo() {
        val modelInfo = AiModelInfo("test-model")
        val trace = AiTaskTrace(null, modelInfo, AiExecInfo(), AiOutputInfo.text("result"))
        assertEquals("test-model", trace.env?.modelId)
        assertNull(trace.input)
        assertEquals("result", trace.firstValue.textContent())
    }

    //endregion

    //region TASK IDENTITY

    @Test
    fun testTaskIdAndParentId() {
        val parent = AiTaskTrace(taskId = "parent-id")
        val child = AiTaskTrace(taskId = "child-id", parentTaskId = parent.taskId)

        assertEquals("parent-id", child.parentTaskId)
        assertEquals("child-id", child.taskId)

        // uuid backward compat
        @Suppress("DEPRECATION")
        assertEquals("parent-id", parent.uuid)
    }

    @Test
    fun testCallerId() {
        val trace = AiTaskTrace(callerId = "prompt-script-view")
        assertEquals("prompt-script-view", trace.callerId)
        // AiTaskId convenience accessor
        assertEquals("prompt-script-view", trace.id.callerId)
    }

    //endregion

    //region COPY METHOD

    @Test
    fun testCopyPreservesTaskId() {
        val trace = AiTaskTrace(taskId = "orig-id", env = AiEnvInfo(model = AiModelInfo("m1")))
        val copied = trace.copy(modelInfo = AiModelInfo("m2"))
        assertEquals("orig-id", copied.taskId)
        assertEquals("m2", copied.env?.modelId)
    }

    @Test
    fun testCopyWithCallerId() {
        val trace = AiTaskTrace()
        val withCaller = trace.copy(callerId = "my-view", execInfo = AiExecInfo(intermediateResult = false))
        assertEquals("my-view", withCaller.callerId)
        assertEquals(false, withCaller.exec.intermediateResult)
    }

    //endregion

    //region FACTORY METHODS

    @Test
    fun testErrorFactory() {
        val trace = AiTaskTrace.error(AiModelInfo("m1"), "something failed", null)
        assertFalse(trace.exec.succeeded())
        assertEquals("something failed", trace.exec.error)
        assertEquals("m1", trace.env?.modelId)
    }

    @Test
    fun testOutputFactory() {
        val trace = AiTaskTrace.output("hello world")
        assertEquals("hello world", trace.firstValue.textContent())
        assertTrue(trace.exec.succeeded())
    }

    //endregion

    //region SERIALIZATION

    @Test
    fun testSerializeBasicTrace() {
        val trace = AiTaskTrace(
            taskId = "test-id",
            env = AiEnvInfo(model = AiModelInfo("gpt-4o")),
            input = AiTaskInputInfo("Hello, world!"),
            exec = AiExecInfo(),
            output = AiOutputInfo.text("Hi!")
        )
        val json = jsonWriter.writeValueAsString(trace)
        assertNotNull(json)
        assertTrue(json.contains("test-id") || json.contains("gpt-4o"))
    }

    @Test
    fun testSerializeWithStats() {
        val trace = AiTaskTrace(
            env = AiEnvInfo(model = AiModelInfo("gpt-4o")),
            exec = AiExecInfo(queryTokens = 10, responseTokens = 50, stats = mapOf("custom_metric" to 42)),
            output = AiOutputInfo.text("test output")
        )
        val json = jsonWriter.writeValueAsString(trace)
        assertNotNull(json)
    }

    //endregion

}
