/*-
 * #%L
 * tri.promptfx:promptfx
 * %%
 * Copyright (C) 2023 - 2024 Johns Hopkins University Applied Physics Laboratory
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
package tri.promptfx.integration

import tri.ai.core.TextCompletion
import tri.ai.core.instructTask
import tri.ai.core.jsonPromptTask
import tri.ai.embedding.EmbeddingService
import tri.ai.embedding.cosineSimilarity
import tri.ai.openai.*
import tri.ai.pips.*
import tri.ai.prompt.AiPromptLibrary
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.util.info

/** Uses OpenAI and a weather API to answer questions about the weather. */
class WeatherAiTaskPlanner(val completionEngine: TextCompletion, val embeddingService: EmbeddingService, val input: String) : AiPlanner {

    override fun plan() =
        aitask("weather-similarity-check") {
            checkWeatherSimilarity(input)
        }.aitask("weather-api-request") {
            completionEngine.jsonPromptTask<WeatherRequest>(AiPromptLibrary.lookupPrompt("weather-api-request"), input, tokenLimit = 500, temp = null)
        }.task("weather-api") {
            weatherService.getWeather(it!!)
        }.aitask("weather-response-formatter") {
            val json = jsonMapper.writeValueAsString(it)
            completionEngine.instructTask(AiPromptLibrary.lookupPrompt("weather-response-formatter"), instruct = input, userText = json, tokenLimit = 500, temp = null)
        }.plan

    private suspend fun checkWeatherSimilarity(input: String): AiPromptTrace<String> {
        val embeddings = embeddingService.calculateEmbedding("is it raining snowing sunny windy in city new york", input)
        val similarity = cosineSimilarity(embeddings[0], embeddings[1])
        info<WeatherAiTaskPlanner>("Input alignment to weather: $similarity")
        if (similarity < 0.5)
            throw IllegalArgumentException("The input is not about weather.")

        return AiPromptTrace(
            modelInfo = AiModelInfo(embeddingService.modelId),
            outputInfo = AiOutputInfo.output(input)
        )
    }

}
