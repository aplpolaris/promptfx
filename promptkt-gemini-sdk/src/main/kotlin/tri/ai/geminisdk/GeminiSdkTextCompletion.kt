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
package tri.ai.geminisdk

import tri.ai.core.TextCompletion
import tri.ai.core.MChatVariation
import tri.ai.prompt.trace.AiPromptTrace
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiOutputInfo
import java.util.Optional

/** Gemini text completion model using the official SDK. */
class GeminiSdkTextCompletion(
    override val modelId: String,
    private val client: GeminiSdkClient
) : TextCompletion {

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop)
        val t0 = System.currentTimeMillis()
        
        try {
            val response = client.generateContent(text, modelId, variation)
            // Handle Optional<String> from java-genai
            val textOptional: Optional<String> = response.text()
            val responseText = if (textOptional.isPresent) textOptional.get() else ""
            
            return AiPromptTrace(
                null,
                modelInfo,
                AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                AiOutputInfo.text(responseText)
            )
        } catch (e: Exception) {
            return AiPromptTrace.error(modelInfo, e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

    override fun toString() = modelId

}
