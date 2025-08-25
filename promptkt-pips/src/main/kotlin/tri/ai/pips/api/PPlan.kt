package tri.ai.pips.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

/** A serializable version of a plan. */
data class PPlan(
    /** The optional ID of the plan. */
    val id: String? = null,
    /** The list of steps in the plan. */
    val steps: List<PPlanStep>
) {
    companion object {
        val MAPPER = ObjectMapper().registerModule(KotlinModule.Builder().build())
        /** Construct plan from JSON. */
        fun parse(json: String) = MAPPER.readValue<PPlan>(json)
        /** An empty plan with no ID and no steps. */
        val EMPTY = PPlan(null, emptyList())
    }
}

/** A serializable version of a plan step. */
data class PPlanStep(
    /** Reference to the tool to be used. */
    val tool: String,
    /** The input to the tool. */
    val input: JsonNode,
    /** Optional location to save tool result. */
    val saveAs: String? = null,
    /** Optional instruction for handling errors. */
    val onError: OnError = OnError.Fail,
    /** Optional timeout for the tool execution in milliseconds. */
    val timeoutMs: Long? = null,
)

/** Instruction for handling errors during tool execution. */
enum class OnError {
    /** Retry the step on error. */
    Retry,
    /** Continue to the next step on error. */
    Continue,
    /** Fail the entire plan execution on error. */
    Fail
}