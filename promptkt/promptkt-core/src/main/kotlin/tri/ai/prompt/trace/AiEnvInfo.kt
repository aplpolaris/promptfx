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

/**
 * Environment and configuration descriptor for an AI task execution.
 *
 * Captures the AI model configuration together with optional environment- and system-level
 * information so that traces can fully reproduce the execution context:
 *
 * - **[model]** — the primary model configuration (ID, source, request parameters).
 * - **[system]** — freeform system/assistant prompt or system-level instructions sent to the model.
 * - **[config]** — open-ended key/value map for any additional environment or runtime settings
 *   not covered by [model] (e.g. API version, SDK version, reasoning strategy, feature flags).
 *
 * Designed to be compatible with OTel/LangFuse span attributes.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiEnvInfo(
    /** Primary model configuration (model ID, source, request parameters). */
    var model: AiModelInfo? = null,
    /** System prompt or system-level instruction, if any. */
    var system: String? = null,
    /**
     * Additional environment or runtime configuration entries.
     * Examples: `"api_version"`, `"sdk_version"`, `"reasoning_effort"`, `"feature_flags"`.
     */
    var config: Map<String, Any> = mapOf()
) {

    // -------------------------------------------------------------------------
    // Convenience accessors that mirror the fields of [AiModelInfo]
    // -------------------------------------------------------------------------

    /** Shortcut for [model]?.modelId. */
    @get:JsonIgnore
    val modelId: String?
        get() = model?.modelId

    /** Shortcut for [model]?.modelSource. */
    @get:JsonIgnore
    val modelSource: String?
        get() = model?.modelSource

    /** Shortcut for [model]?.modelParams. */
    @get:JsonIgnore
    val modelParams: Map<String, Any>
        get() = model?.modelParams ?: mapOf()

    // -------------------------------------------------------------------------
    // Companion factory methods
    // -------------------------------------------------------------------------

    companion object {

        /** Creates an [AiEnvInfo] from a single [AiModelInfo]. */
        fun of(modelInfo: AiModelInfo) = AiEnvInfo(model = modelInfo)

        /** Creates an [AiEnvInfo] with just a model ID (no extra params). */
        fun of(modelId: String) = AiEnvInfo(model = AiModelInfo(modelId))

        /** Creates an [AiEnvInfo] with a model and a system prompt. */
        fun of(modelInfo: AiModelInfo, system: String) = AiEnvInfo(model = modelInfo, system = system)
    }
}
