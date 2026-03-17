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
package tri.ai.pips.api

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import tri.ai.core.tool.ExecContext
import tri.ai.core.tool.Executable
import tri.ai.core.tool.ExecutableRegistry
import tri.ai.pips.AiWorkflowExecutor
import tri.ai.pips.AiTask
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import tri.util.info
import tri.util.json.jsonMapper

/**
 * Executes a [PPlan] by converting to a series of [AiTask<*>] objects and using [tri.ai.pips.AiWorkflowExecutor].
 * Supports monitoring intermediate status.
 */
class PPlanExecutor(private val registry: ExecutableRegistry) {

    suspend fun execute(plan: PPlan, context: ExecContext = ExecContext()) {
        PPlanValidator.validateNames(plan)
        PPlanValidator.validateToolsExist(plan, registry)
        PPlanValidator.validateHasSteps(plan)

        val tasks = plan.steps.map { step ->
            val exec = registry.get(step.tool)
                ?: throw IllegalArgumentException("No executable found for ${step.tool}")
            AiPlanStepTask(step, exec, context)
        }
        AiWorkflowExecutor.execute(tasks, context)
    }

}

/** An executable task for a plan step. */
class AiPlanStepTask(val step: PPlanStep, private val exec: Executable, private val stepContext: ExecContext) :
    AiTask<Any?, Any?>(step.tool, description = null, dependencies = setOf()) {

    override suspend fun execute(input: Any?, context: ExecContext): Any {
        val jsonScratchpad = stepContext.jsonScratchpad()
        log("context", jsonScratchpad)

        val inputMap = step.input.resolveRefs(jsonScratchpad)
        log("input", inputMap)

        val result = exec.execute(inputMap, stepContext)
        log("output", result)

        step.saveAs?.let { stepContext.put(it, result) }
        return result
    }

    private fun log(label: String, node: JsonNode) {
        if (node.isTextual)
            info<PPlanExecutor>("$ANSI_GRAY  $label: ${node.asText()}$ANSI_RESET")
        else
            log(label, jsonMapper.convertValue<Map<String, Any?>>(node))
    }

    private fun log(label: String, node: Map<String, Any?>) {
        info<PPlanExecutor>("$ANSI_GRAY  $label:$ANSI_RESET")
        node.forEach { (k, v) -> info<PPlanExecutor>("$ANSI_GRAY    $k: $v$ANSI_RESET") }
    }

    companion object {
        /** Resolve any ref object in the tree such as { "$ref": "varName" } with a reference lookup in the vars table. */
        private fun JsonNode.resolveRefs(vars: Map<String, JsonNode>): JsonNode {
            val mapper = ObjectMapper()

            fun select(base: JsonNode, ptr: String?): JsonNode =
                if (ptr.isNullOrEmpty()) base else base.at(JsonPointer.valueOf(ptr))

            fun resolve(node: JsonNode, stack: MutableSet<String>): JsonNode {
                // Reference node?
                if (node.isObject) {
                    // Minimal shape: {"$var":"name"} or {"$var":"name","$ptr":"/text"}
                    val varField = node.get("\$var")
                    if (varField != null && varField.isTextual) {
                        val name = varField.asText()
                        val target = when {
                            name !in vars -> throw IllegalArgumentException("Unknown \$var: $name (variable not found in context)")
                            else -> vars[name]!!
                        }
                        if (!stack.add(name)) error("Cyclic \$var detected at: $name")
                        val selected = select(target, node.get("\$ptr")?.takeIf { it.isTextual }?.asText())
                        val resolved = resolve(selected, stack) // allow nested refs inside target
                        stack.remove(name)
                        return resolved.deepCopy<JsonNode>()
                    }
                }
                // Recurse objects
                if (node.isObject) {
                    val out: ObjectNode = mapper.createObjectNode()
                    val fields = node.fields()
                    while (fields.hasNext()) {
                        val (k, v) = fields.next()
                        out.set<JsonNode>(k, resolve(v, stack))
                    }
                    return out
                }
                // Recurse arrays
                if (node.isArray) {
                    val out: ArrayNode = mapper.createArrayNode()
                    node.forEach { out.add(resolve(it, stack)) }
                    return out
                }
                // Primitives/null
                return node.deepCopy<JsonNode>()
            }

            return resolve(this, mutableSetOf())
        }
    }
}
