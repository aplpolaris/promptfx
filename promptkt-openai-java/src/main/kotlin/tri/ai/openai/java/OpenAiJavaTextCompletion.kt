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
package tri.ai.openai.java

import tri.ai.core.MChatVariation
import tri.ai.core.TextCompletion
import tri.ai.openai.java.OpenAiJavaModelIndex.GPT_4O_MINI
import tri.ai.prompt.trace.AiExecInfo
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace

/** Text completion with OpenAI models using the official Java SDK. */
class OpenAiJavaTextCompletion(
    override val modelId: String = GPT_4O_MINI,
    val client: OpenAiJavaClient = OpenAiJavaClient.INSTANCE
) : TextCompletion {

    override fun toString() = "$modelId (OpenAI Java SDK)"

    override suspend fun complete(
        text: String,
        variation: MChatVariation,
        tokens: Int?,
        stop: List<String>?,
        numResponses: Int?
    ): AiPromptTrace {
        val modelInfo = AiModelInfo.info(modelId, tokens = tokens, stop = stop, numResponses = numResponses)
        val t0 = System.currentTimeMillis()

        val paramsBuilder = com.openai.models.chat.completions.ChatCompletionCreateParams.builder()
            .model(modelId)
            .addMessage(com.openai.models.chat.completions.ChatCompletionUserMessageParam.ofTextContent(text))
            .maxCompletionTokens(tokens?.toLong() ?: 500)
        
        variation.temperature?.let { paramsBuilder.temperature(it.toDouble()) }
        variation.topP?.let { paramsBuilder.topP(it.toDouble()) }
        variation.seed?.let { paramsBuilder.seed(it.toLong()) }
        variation.presencePenalty?.let { paramsBuilder.presencePenalty(it.toDouble()) }
        variation.frequencyPenalty?.let { paramsBuilder.frequencyPenalty(it.toDouble()) }
        
        stop?.let { paramsBuilder.stop(com.openai.models.chat.completions.ChatCompletionCreateParams.Stop.ofStrings(it)) }
        numResponses?.let { paramsBuilder.n(it.toLong()) }

        val completion = client.client.chat().completions().create(paramsBuilder.build())
        
        val responseTexts = completion.choices().map { choice ->
            choice.message().content().orElse("")
        }

        return AiPromptTrace(
            null,
            modelInfo,
            AiExecInfo(
                responseTimeMillis = System.currentTimeMillis() - t0,
                queryTokens = completion.usage().map { it.promptTokens().toInt() }.orElse(null),
                responseTokens = completion.usage().map { it.completionTokens().toInt() }.orElse(null)
            ),
            AiOutputInfo.text(responseTexts)
        )
    }

}
