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

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import tri.ai.pips.AiPipelineExecutor
import tri.ai.pips.AiPlanner
import tri.ai.pips.AiTask
import tri.ai.pips.AiTaskMonitor
import tri.ai.pips.PrintMonitor
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiPromptTraceSupport
import tri.util.ANSI_GRAY
import tri.util.ANSI_RESET
import tri.util.info

/** Executes a [PPlan] by converting to a series of [AiTask<*>] objects and using [tri.ai.pips.AiPipelineExecutor]. */
class PPlanExecutor(private val registry: PExecutableRegistry) {

    suspend fun execute(plan: PPlan, context: PExecContext = PExecContext()) {
        PPlanValidator.validateNames(plan)
        PPlanValidator.validateToolsExist(plan, registry)

        val planner = PPlanPlanner(plan, context, registry)
        val tasks = planner.plan()
        AiPipelineExecutor.execute(tasks, PrintMonitor())
    }

}

/** Converts a [PPlan] to a series of [AiTask<*>] objects. */
class PPlanPlanner(
    private val plan: PPlan,
    private val context: PExecContext = PExecContext(),
    private val registry: PExecutableRegistry
) : AiPlanner {

    override fun plan(): List<AiTask<*>> {
        return plan.steps.map { step ->
            val exec = registry.get(step.tool)
                ?: throw IllegalArgumentException("No executable found for ${step.tool}")
            object : AiTask<Any?>(step.tool, description = null, dependencies = setOf()) {
                override suspend fun execute(
                    inputs: Map<String, AiPromptTraceSupport<*>>,
                    monitor: AiTaskMonitor
                ): AiPromptTraceSupport<Any?> {
                    info<PPlanExecutor>("$ANSI_GRAY  context vars:$ANSI_RESET")
                    context.vars.forEach { (k, v) -> info<PPlanExecutor>("$ANSI_GRAY    $k: $v$ANSI_RESET") }

                    val inputMap = step.input.resolveRefs(context.vars)
                    info<PPlanExecutor>("$ANSI_GRAY  input:$ANSI_RESET")
                    PPlan.MAPPER.convertValue<Map<String, Any>>(inputMap)
                        .forEach { (k, v) -> info<PPlanExecutor>("$ANSI_GRAY    $k: $v$ANSI_RESET") }

                    val result = exec.execute(inputMap, context)
                    step.saveAs?.let { context.vars[it] = result }
                    info<PPlanExecutor>("$ANSI_GRAY  output:$ANSI_RESET")
                    PPlan.MAPPER.convertValue<Map<String, Any?>>(result)
                        .forEach { (k, v) -> info<PPlanExecutor>("$ANSI_GRAY    $k: $v$ANSI_RESET") }

                    return AiPromptTrace.output(result)
                }
            }
        }
    }

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
                    val target = vars[name] ?: throw IllegalArgumentException("Unknown \$var: $name")
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
