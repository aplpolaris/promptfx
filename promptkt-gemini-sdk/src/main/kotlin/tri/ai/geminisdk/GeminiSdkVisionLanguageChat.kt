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

import tri.ai.core.*
import tri.ai.prompt.trace.*

/** Gemini vision language chat model using the official SDK. */
class GeminiSdkVisionLanguageChat(
    override val modelId: String,
    private val client: GeminiSdkClient
) : VisionLanguageChat {

    override suspend fun chat(
        messages: List<VisionLanguageChatMessage>,
        temp: Double?,
        tokens: Int?,
        stop: List<String>?,
        requestJson: Boolean?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, requestJson = requestJson)
        val t0 = System.currentTimeMillis()
        
        if (messages.isEmpty()) {
            return AiPromptTrace.error(modelInfo, "No messages provided", duration = System.currentTimeMillis() - t0)
        }
        
        val variation = MChatVariation(temperature = temp)
        
        try {
            val response = client.generateContentVision(
                messages,
                modelId,
                variation
            )
            
            // Handle nullable String from java-genai (platform type String!)
            val responseText = response.text() ?: ""
            
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
