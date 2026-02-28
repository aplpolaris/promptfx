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
package tri.ai.geminisdk

import tri.ai.core.*
import tri.ai.geminisdk.GeminiSdkClient.Companion.extractTexts
import tri.ai.prompt.trace.*

/** Gemini text completion model using the official SDK. */
class GeminiSdkTextCompletion(
    override val modelId: String,
    private val client: GeminiSdkClient
) : TextCompletion {

    override val modelSource = "Gemini-SDK"

    override fun toString() = modelDisplayName()

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop)
        val t0 = System.currentTimeMillis()
        
        return try {
            val message = MultimodalChatMessage.text(MChatRole.User, text)
            val response = client.generateContent(modelId, variation, listOf(message), tools = null, numResponses ?: 1)
            val responseTexts = response.extractTexts(numResponses ?: 1)
            
            AiPromptTrace(
                null,
                modelInfo,
                AiExecInfo(responseTimeMillis = System.currentTimeMillis() - t0),
                if (responseTexts.size == 1) AiOutputInfo.text(responseTexts.first()) else AiOutputInfo.text(responseTexts)
            )
        } catch (e: Exception) {
            AiPromptTrace.error(modelInfo, e.message ?: "Unknown error", duration = System.currentTimeMillis() - t0)
        }
    }

}
