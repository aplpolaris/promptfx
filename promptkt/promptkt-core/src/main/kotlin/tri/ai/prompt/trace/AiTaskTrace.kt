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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import tri.ai.core.TextChatMessage
import java.util.UUID.randomUUID

/**
 * Unified execution trace for AI tasks, capturing task identity, model/environment configuration,
 * task inputs, execution statistics, and output values.
 *
 * Designed to support a wide range of task types, including:
 * - Prompt template tasks (text completion, chat, vision, speech)
 * - Agentic reasoning workflows (with parent/child task relationships)
 * - Batch processing and pipeline steps
 *
 * Compatible with OTel/LangFuse trace concepts.
 *
 * Task identity is captured by [taskId], [parentTaskId], and [callerId], which are
 * also grouped together as [id] (an [AiTaskId]).
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
open class AiTaskTrace(
    /** Unique identifier for this trace. */
    var taskId: String = randomUUID().toString(),
    /** Parent task identifier, enabling reconstruction of call graphs. */
    var parentTaskId: String? = null,
    /** Identifier of the caller or system component that initiated this task. */
    var callerId: String? = null,
    /** Model and environment configuration. */
    var env: AiEnvInfo? = null,
    /** Task input information. */
    var input: AiTaskInputInfo? = null,
    /** Execution metadata (timing, errors, token usage). */
    var exec: AiExecInfo = AiExecInfo(),
    /** Task output values. */
    var output: AiOutputInfo? = null
) {

    // region TASK IDENTITY

    /**
     * Returns the task identity as an [AiTaskId] grouping [taskId], [parentTaskId], and [callerId].
     * This is a convenience accessor; the fields are stored directly on the trace.
     */
    @get:JsonIgnore
    val id: AiTaskId
        get() = AiTaskId(taskId, parentTaskId, callerId)

    // endregion

    // region COMPUTED PROPERTIES

    /** Returns all output values, or null if no output is present. */
    @get:JsonIgnore
    val values: List<AiOutput>?
        get() = output?.outputs

    /** Returns the first output value, or throws [NoSuchElementException] if none exists. */
    @get:JsonIgnore
    val firstValue: AiOutput
        get() = output?.outputs?.firstOrNull() ?: throw NoSuchElementException("No output value")

    /** Returns the error message, if any. */
    @get:JsonIgnore
    val errorMessage: String?
        get() = exec.error ?: exec.throwable?.message

    /**
     * Open-ended map for storing non-serialized, in-process side-channel data for the outputs of
     * this trace (e.g. formatted text, rendering hints). Delegates to [output]`.annotations`.
     * Not serialized to JSON; lost when the trace is persisted or copied.
     *
     * Returns an empty map and discards writes if [output] is null.
     */
    @get:JsonIgnore
    val annotations: MutableMap<String, Any>
        get() = output?.annotations ?: mutableMapOf()

    // endregion

    // region COPY / TRANSFORM

    /**
     * Creates a copy of this trace with optionally updated fields.
     * Parameter order matches the primary constructor.
     */
    open fun copy(
        taskId: String = this.taskId,
        parentTaskId: String? = this.parentTaskId,
        callerId: String? = this.callerId,
        env: AiEnvInfo? = this.env,
        input: AiTaskInputInfo? = this.input,
        exec: AiExecInfo = this.exec,
        output: AiOutputInfo? = this.output
    ): AiTaskTrace = AiTaskTrace(taskId, parentTaskId, callerId, env, input, exec, output)

    /** Returns a copy of this trace with the output transformed by [transform]. */
    fun mapOutput(transform: (AiOutput) -> AiOutput) = copy(output = output?.map(transform))

    // endregion

    override fun toString() =
        "AiTaskTrace(taskId=$taskId, callerId=$callerId, env=$env, input=$input, exec=$exec, output=$output)"

    // region COMPANION FACTORY METHODS

    companion object {

        /** Creates a trace representing a failed execution. */
        fun error(
            modelInfo: AiModelInfo?,
            message: String?,
            throwable: Throwable? = null,
            duration: Long? = null,
            durationTotal: Long? = null,
            attempts: Int? = null
        ) = AiTaskTrace(
            env = modelInfo?.let { AiEnvInfo.of(it) },
            exec = AiExecInfo(message, throwable, buildMap {
                duration?.let { put(AiExecInfo.RESPONSE_TIME_MILLIS, it) }
                durationTotal?.let { put(AiExecInfo.RESPONSE_TIME_MILLIS_TOTAL, it) }
                attempts?.let { put(AiExecInfo.ATTEMPTS, it) }
            })
        )

        /** Creates a trace representing a request that was not attempted because the model ID was invalid. */
        fun invalidRequest(modelId: String, message: String) =
            invalidRequest(AiModelInfo(modelId), message)

        /** Creates a trace representing a request that was not attempted because the input was invalid. */
        fun invalidRequest(modelInfo: AiModelInfo?, message: String) =
            error(modelInfo, message, IllegalArgumentException(message))

        /** Creates a trace wrapping a single [AiOutput]. */
        fun output(output: AiOutput) = AiTaskTrace(output = AiOutputInfo(listOf(output)))

        /** Creates a trace wrapping a single text output. */
        fun output(output: String) = AiTaskTrace(output = AiOutputInfo.text(output))

        /** Creates a trace wrapping a list of text outputs. */
        fun output(output: List<String>) = AiTaskTrace(output = AiOutputInfo.text(output))

        /** Creates a trace wrapping a single chat message output. */
        fun outputMessage(output: TextChatMessage) = AiTaskTrace(output = AiOutputInfo.message(output))

        /** Wraps a series of outputs as a single list output object. */
        fun outputListAsSingleResult(list: List<AiOutput>) =
            output(AiOutput.Other(list.map { it.content() }))

    }

    // endregion

}
