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
package tri.ai.prompt.trace

import com.fasterxml.jackson.annotation.JsonInclude

/** Model configuration info. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class AiModelInfo(
    var modelId: String,
    var modelParams: Map<String, Any> = mapOf()
) {
    companion object {
        const val MAX_TOKENS = "max_tokens"
        const val TEMPERATURE = "temperature"
        const val TOP_P = "top_p"
        const val NUM_RESPONSES = "n"
        const val LOG_PROBS = "logprobs"
        const val TOP_LOG_PROBS = "top_logprobs"
        const val ECHO = "echo"
        const val STOP = "stop"
        const val PRESENCE_PENALTY = "presence_penalty"
        const val FREQUENCY_PENALTY = "frequency_penalty"
        const val BEST_OF = "best_of"
        const val LOGIT_BIAS = "logit_bias"
        const val USER = "user"
        const val SUFFIX = "suffix"
        const val RESPONSE_FORMAT = "response_format"
        const val SEED = "seed"
        const val OUTPUT_DIMENSIONS = "dimensions"
        const val SIZE = "size"
        const val QUALITY = "quality"
        const val STYLE = "style"
        const val VOICE = "voice"
        const val SPEED = "speed"
        const val EMBEDDING_MODEL = "embedding_model"
        const val CHUNKER_ID = "chunker_id"
        const val CHUNKER_MAX_CHUNK_SIZE = "chunker_max_chunk_size"

        /** Create model info. */
        fun info(modelId: String, vararg pairs: Pair<String, Any?>) =
            AiModelInfo(modelId, mapOfNotNull(*pairs))

        /** Create model info. */
        fun info(modelId: String, tokens: Int? = null, stop: List<String>? = null, requestJson: Boolean? = null, numResponses: Int? = null) =
            AiModelInfo(modelId,
                mapOfNotNull(MAX_TOKENS to tokens, STOP to stop, NUM_RESPONSES to numResponses, "request_json" to requestJson)
            )

        /** Create embedding model info. */
        fun embedding(modelId: String, outputDims: Int? = null) = AiModelInfo(modelId, mapOfNotNull(OUTPUT_DIMENSIONS to outputDims))

        private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
            mapOf(*pairs).filterValues { it != null } as Map<String, Any>
    }
}
