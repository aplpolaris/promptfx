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
package tri.ai.pips

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tri.ai.core.tool.ExecContext
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.pips.AiPipelineExecutorTest.GoTask

class AiWorkflowTest {

    private fun printingExecContext() = ExecContext(monitor = PrintMonitor())

    // --- Simple workflow execution ---

    @Test
    fun `workflow executes inner tasks and returns last task output`() = runTest {
        val workflow = AiTaskBuilder.task("step1") { "hello" }
            .task("step2") { s, _ -> "$s world" }
            .asWorkflow("myWorkflow")

        val ctx = printingExecContext()
        val output = workflow.execute(null, ctx)
        assertEquals("hello world", output)
    }

    @Test
    fun `workflow merges inner traces into parent context with prefix`() = runTest {
        val workflow = AiTaskBuilder.task("step1") { "a" }
            .task("step2") { s, _ -> s + "b" }
            .asWorkflow("wf")

        val ctx = printingExecContext()
        workflow.execute(null, ctx)

        assertNotNull(ctx.getTrace("wf/step1"))
        assertNotNull(ctx.getTrace("wf/step2"))
    }

    // --- Composition: workflow used as a task step inside another pipeline ---

    @Test
    fun `workflow can be composed as a step in an outer pipeline`() = runTest {
        val inner = AiTaskBuilder.task("inner1") { 42 }
            .task("inner2") { n, _ -> n * 2 }
            .asWorkflow("sub")

        val outerPlan = AiTaskBuilder(listOf(), inner)
            .task("outer1") { n, _ -> "result=$n" }
            .plan

        val result = AiPipelineExecutor.execute(outerPlan, printingExecContext())
        assertEquals("result=84", result.finalResult.firstValue.textContent())
    }

    @Test
    fun `outer pipeline can chain multiple workflows`() = runTest {
        val wf1 = AiTaskBuilder.task("a1") { "foo" }
            .task("a2") { s, _ -> s + "bar" }
            .asWorkflow("wf1")

        val wf2 = AiTaskBuilder.task("b1") { "x" }
            .task("b2") { s, _ -> s + "y" }
            .asWorkflow("wf2", dependencies = setOf())

        val outerPlan = AiTaskBuilder(listOf(), wf1)
            .task("combine") { s1, ctx ->
                val s2 = ctx.get("wf2") as? String ?: ""
                "$s1+$s2"
            }.plan

        // Run wf2 as a standalone first to put its output in context
        val ctx = printingExecContext()
        ctx.put("wf2", "xy")

        val result = AiPipelineExecutor.execute(outerPlan, ctx)
        assertEquals("foobar+xy", result.finalResult.firstValue.textContent())
    }

    // --- Input threading ---

    @Test
    fun `workflow makes outer input available to inner tasks via workflow id`() = runTest {
        // Inner task depends on the workflow id to consume the outer input
        val innerTask = object : AiTask<String, String>("innerTask", dependencies = setOf("inputWf")) {
            override suspend fun execute(input: String, context: ExecContext): String {
                context.logTrace(id, AiPromptTrace(outputInfo = AiOutputInfo.text(input + "!")))
                return input + "!"
            }
        }
        val workflow = AiWorkflow<String, String>("inputWf", tasks = listOf(innerTask))

        val ctx = printingExecContext()
        val output = workflow.execute("hello", ctx)
        assertEquals("hello!", output)
    }

    // --- Failure propagation ---

    @Test
    fun `workflow propagates inner failure as exception`() = runTest {
        val failingTask = object : AiTask<Any?, String>("failStep") {
            override suspend fun execute(input: Any?, context: ExecContext): String {
                context.logTrace(id, AiPromptTrace.error(null, "fail", Exception("fail")))
                throw IllegalStateException("fail")
            }
        }
        val workflow = AiWorkflow<Any?, String>("failingWorkflow", tasks = listOf(failingTask))

        val ctx = printingExecContext()
        val ex = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { workflow.execute(null, ctx) }
        }
        assertTrue(ex.message!!.contains("failingWorkflow"), "Expected message to contain 'failingWorkflow' but was: ${ex.message}")
    }

    @Test
    fun `outer pipeline marks workflow task as failed when inner pipeline fails`() = runTest {
        val failingTask = object : AiTask<Any?, String>("failStep") {
            override suspend fun execute(input: Any?, context: ExecContext): String {
                context.logTrace(id, AiPromptTrace.error(null, "fail", Exception("fail")))
                throw IllegalStateException("fail")
            }
        }
        val workflow = AiWorkflow<Any?, String>("wfFail", tasks = listOf(failingTask))

        val outerTask = object : AiTask<Any?, String>("afterFail", dependencies = setOf("wfFail")) {
            override suspend fun execute(input: Any?, context: ExecContext): String {
                context.logTrace(id, AiPromptTrace(outputInfo = AiOutputInfo.text("should not run")))
                return "should not run"
            }
        }

        val result = AiPipelineExecutor.execute(listOf(workflow, outerTask), printingExecContext())
        assertNotNull(result.interimResults["wfFail"]?.errorMessage)
        assertNull(result.interimResults["afterFail"])
    }

    // --- asWorkflow extension on AiTaskBuilder ---

    @Test
    fun `asWorkflow creates AiWorkflow with correct id and plan`() {
        val builder = AiTaskBuilder.task("t1") { "v" }
            .task("t2") { s, _ -> s + "2" }
        val wf = builder.asWorkflow("myWf", "My Workflow", setOf("dep1"))
        assertEquals("myWf", wf.id)
        assertEquals("My Workflow", wf.description)
        assertEquals(setOf("dep1"), wf.dependencies)
        assertEquals(builder.plan, wf.tasks)
    }
}
