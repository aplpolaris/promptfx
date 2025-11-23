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
package tri.ai.core.agent.wf

import tri.ai.core.agent.impl.PROMPTS
import tri.ai.openai.OpenAiCompletionChat
import tri.ai.prompt.template
import tri.util.json.createJsonSchema
import tri.util.json.createObject

/** Solver that takes a single input, provides a single output, based on a runner. */
class RunSolver(
    name: String,
    description: String,
    version: String = "",
    inputDescription: String,
    outputDescription: String,
    val run: suspend (String) -> String
) : WorkflowSolver(name, description, version, createJsonSchema(INPUT to inputDescription), createJsonSchema(RESULT to outputDescription)) {

    override suspend fun solve(state: WorkflowState, task: WorkflowTask): WorkflowSolveStep {
        val t0 = System.currentTimeMillis()
        val inputData = state.aggregateInputsAsStringFor(name, task.name)
        val result = run(inputData)
        return WorkflowSolveStep(
            task,
            this,
            createObject(INPUT, inputData),
            createObject(RESULT, result),
            System.currentTimeMillis() - t0,
            true
        )
    }

}

/** Solver that takes a single input, provides a single output, based on an LLM chat request. */
fun chatSolver(name: String, description: String, version: String, inputDescription: String, outputDescription: String, promptId: String) =
    RunSolver(name, description, version, inputDescription, outputDescription) { inputData ->
        val prompt = PROMPTS.get(promptId)!!.template().fillInput(inputData)
        val result = OpenAiCompletionChat().complete(prompt, tokens = 1000)
        result.firstValue.textContent()
    }

/** Solver that takes a single input, provides a single output, based on an LLM chat request. */
fun instructSolver(name: String, description: String, version: String, inputDescription: String, outputDescription: String, instruction: String) =
    RunSolver(name, description, version, inputDescription, outputDescription) { inputData ->
        val promptInput = "$instruction\n\nInput:\n$inputData"
        val result = OpenAiCompletionChat().complete(promptInput, tokens = 1000)
        result.firstValue.textContent()
    }