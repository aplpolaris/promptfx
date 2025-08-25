package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
                    info<PPlanExecutor>("$ANSI_GRAY  context vars: ${context.vars}$ANSI_RESET")
                    val inputMap = step.input.resolveRefs(context.vars)
                    info<PPlanExecutor>("$ANSI_GRAY  input: ${inputMap}$ANSI_RESET")
                    val inputNode = PPlan.MAPPER.valueToTree<JsonNode>(inputMap)
                    val result = exec.execute(inputNode, context)
                    step.saveAs?.let { context.vars[it] = result }
                    return AiPromptTrace.output(result)
                }
            }
        }
    }

    /** Resolve any ref object in the tree such as { "$ref": "varName" } with a reference lookup in the vars table. */
    private fun JsonNode.resolveRefs(vars: Map<String, JsonNode>): JsonNode {
        fun resolve(node: JsonNode, stack: MutableSet<String>): JsonNode {
            // Reference node? Allow "$ref" (primary) and "$var" (alias)
            if (node.isObject && node.size() == 1) {
                val refName = when {
                    node.has("\$ref") && node.get("\$ref").isTextual -> node.get("\$ref").asText()
                    node.has("\$var") && node.get("\$var").isTextual -> node.get("\$var").asText()
                    else -> null
                }
                if (refName != null) {
                    val target = vars[refName] ?: throw IllegalArgumentException("Unknown \$ref: $refName")
                    if (!stack.add(refName)) throw IllegalStateException("Cyclic \$ref detected at: $refName")
                    val resolved = resolve(target, stack)
                    stack.remove(refName)
                    return resolved.deepCopy<JsonNode>()
                }
            }

            return when {
                node.isObject -> {
                    val out: ObjectNode = PPlan.MAPPER.createObjectNode()
                    val fields = node.fields()
                    while (fields.hasNext()) {
                        val (k, v) = fields.next()
                        out.set<JsonNode>(k, resolve(v, stack))
                    }
                    out
                }
                node.isArray -> {
                    val out: ArrayNode = PPlan.MAPPER.createArrayNode()
                    node.forEach { out.add(resolve(it, stack)) }
                    out
                }
                else -> node.deepCopy() // primitives, nulls, etc.
            }
        }

        return resolve(this, mutableSetOf())
    }

}