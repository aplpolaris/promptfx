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
 * Merges the functionality of the former [AiPromptTraceSupport] and [AiPromptTrace].
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
open class AiTaskTrace(
    /** Unique identifier for this trace (formerly [uuid]). */
    var taskId: String = randomUUID().toString(),
    /** Parent task identifier, enabling reconstruction of call graphs. */
    var parentTaskId: String? = null,
    /** Identifier of the view or system component that initiated this task. */
    var viewId: String? = null,
    /** Model and environment configuration. Replaces the former bare [AiModelInfo] field. */
    var env: AiEnvInfo? = null,
    /** Task input information. Replaces the former prompt/params as [AiTaskInputInfo]. */
    var input: AiTaskInputInfo? = null,
    /** Execution metadata (timing, errors, token usage). */
    var exec: AiExecInfo = AiExecInfo(),
    /** Task output values. */
    var output: AiOutputInfo? = null
) {

    // region BACKWARD COMPATIBILITY

    /**
     * Backward-compatible constructor accepting the original [AiPromptTrace] parameters.
     * Converts [PromptInfo] to [AiTaskInputInfo] and wraps [AiModelInfo] in [AiEnvInfo] automatically.
     * New code should prefer the primary constructor or factory methods.
     */
    @Suppress("DEPRECATION")
    constructor(
        promptInfo: PromptInfo?,
        modelInfo: AiModelInfo? = null,
        execInfo: AiExecInfo = AiExecInfo(),
        outputInfo: AiOutputInfo? = null
    ) : this(
        env = modelInfo?.let { AiEnvInfo.of(it) },
        input = promptInfo?.let { AiTaskInputInfo.of(it) },
        exec = execInfo,
        output = outputInfo
    )

    /**
     * Backward-compatible alias for [taskId] (formerly the unique trace identifier stored as [uuid]).
     */
    @Deprecated("Use taskId", ReplaceWith("taskId"))
    var uuid: String
        get() = taskId
        set(value) { taskId = value }

    /**
     * Backward-compatible access to the prompt information, derived from [input].
     * Returns null if [input] has no prompt template.
     */
    @get:JsonIgnore
    @Deprecated("Use input", ReplaceWith("input"))
    var prompt: PromptInfo?
        get() = input?.toPromptInfo()
        set(value) { input = value?.let { AiTaskInputInfo.of(it) } }

    /**
     * Backward-compatible access to the [AiModelInfo], derived from [env].
     * Setting this property wraps the value in an [AiEnvInfo], preserving any existing
     * [AiEnvInfo.system] and [AiEnvInfo.config] settings.
     */
    @get:JsonIgnore
    @Deprecated("Use env", ReplaceWith("env"))
    var model: AiModelInfo?
        get() = env?.model
        set(value) { env = value?.let { env?.copy(model = it) ?: AiEnvInfo.of(it) } }

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

    // endregion

    // region COPY / TRANSFORM

    /**
     * Creates a copy of this trace with optionally updated fields.
     *
     * The [promptInfo] and [modelInfo] parameters are provided for backward compatibility.
     * [modelInfo] is wrapped in an [AiEnvInfo] that inherits any existing [AiEnvInfo.system]
     * and [AiEnvInfo.config] from the current [env].
     * New code should use [envInfo] directly.
     */
    open fun copy(
        promptInfo: PromptInfo? = this.prompt,
        modelInfo: AiModelInfo? = this.model,
        execInfo: AiExecInfo = this.exec,
        outputInfo: AiOutputInfo? = this.output,
        viewId: String? = this.viewId,
        parentTaskId: String? = this.parentTaskId
    ): AiTaskTrace = AiTaskTrace(
        taskId = taskId,
        parentTaskId = parentTaskId,
        viewId = viewId,
        env = modelInfo?.let { env?.copy(model = it) ?: AiEnvInfo.of(it) },
        input = promptInfo?.let { AiTaskInputInfo.of(it) },
        exec = execInfo,
        output = outputInfo
    )

    /** Returns a copy of this trace with the output transformed by [transform]. */
    fun mapOutput(transform: (AiOutput) -> AiOutput) = copy(outputInfo = output?.map(transform))

    // endregion

    override fun toString() =
        "AiTaskTrace(taskId=$taskId, env=$env, input=$input, exec=$exec, output=$output)"

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
            exec = AiExecInfo(message, throwable, responseTimeMillis = duration, responseTimeMillisTotal = durationTotal, attempts = attempts)
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
