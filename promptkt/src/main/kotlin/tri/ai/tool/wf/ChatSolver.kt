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
package tri.ai.tool.wf

import tri.ai.openai.OpenAiCompletionChat
import tri.ai.prompt.AiPromptLibrary

val PROMPTS = AiPromptLibrary.readResource<WorkflowExecutor>()

/** Solver that takes a single input, provides a single output, based on an LLM chat request. */
class ChatSolver(
    name: String,
    description: String,
    inputDescription: String,
    outputDescription: String,
    val promptId: String
) : WorkflowSolver(name, description, mapOf("input" to inputDescription), mapOf("result" to outputDescription)) {
    override suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep {
        val inputs = state.aggregateInputsFor(name).values.mapNotNull { it?.value }.ifEmpty {
            listOf(task.name)
        }
        val inputData = inputs.joinToString("\n")
        val prompt = PROMPTS.fill(promptId, "input" to inputData)
        val result = OpenAiCompletionChat().complete(prompt, tokens = 1000)

        return solveStep(
            task,
            inputs(inputData),
            outputs(result.firstValue),
            result.exec.responseTimeMillisTotal ?: 0L,
            true
        )
    }

}

/** Solver that takes a single input, provides a single output, based on a provided LLM chat instruction. */
class InstructSolver(
    name: String,
    description: String,
    inputDescription: String,
    outputDescription: String,
    val instruction: String
) : WorkflowSolver(name, description, mapOf("input" to inputDescription), mapOf("result" to outputDescription)) {
    override suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep {
        val inputs = state.aggregateInputsFor(name).values.mapNotNull { it?.value }.ifEmpty {
            listOf(task.name)
        }
        val inputData = inputs.joinToString("\n")
        val promptInput = "$instruction\n\nInput:\n$inputData"
        val result = OpenAiCompletionChat().complete(promptInput, tokens = 1000)

        return solveStep(
            task,
            inputs(inputData),
            outputs(result.firstValue),
            result.exec.responseTimeMillisTotal ?: 0L,
            true
        )
    }
}

