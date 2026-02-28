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
package tri.promptfx.`fun`

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import tri.ai.core.CompletionBuilder
import tri.ai.core.CompletionBuilder.Companion.JSON_MAPPER
import tri.ai.core.EmbeddingModel
import tri.ai.core.TextChat
import tri.ai.core.TextCompletion
import tri.ai.embedding.cosineSimilarity
import tri.util.json.jsonMapper
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutput
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.ModelParameters
import tri.promptfx.PromptFxGlobals.lookupPrompt
import tri.util.fine
import tri.util.info

/** Uses OpenAI and a weather API to answer questions about the weather. */
class WeatherAiTaskPlanner(val chatEngine: TextChat, val common: ModelParameters, val embeddingModel: EmbeddingModel, val input: String) : AiPlanner {

    override fun plan() =
        aitask("weather-similarity-check") {
            checkWeatherSimilarity(input)
        }.aitask("weather-api-request") {
            CompletionBuilder()
                .prompt(lookupPrompt("examples-api/weather-api-request"))
                .paramsInput(input)
                .tokens(500)
                .executeJson<WeatherRequest>(chatEngine)
        }.objtask<WeatherRequest, WeatherResult>("weather-api") {
            weatherService.getWeather(it)
        }.aitask("weather-response-formatter") {
            val json = jsonMapper.writeValueAsString(it)
            CompletionBuilder()
                .prompt(lookupPrompt("examples-api/weather-response-formatter"))
                .paramsInstruct(instruct = input, input = json)
                .execute(chatEngine)
        }.plan

    /**
     * Executes a [TextCompletion] task with the provided parameters, and attempts to parse the response as JSON.
     * Fails silently, returning null without throwing an exception if parsing fails.
     */
    private suspend inline fun <reified T> CompletionBuilder.executeJson(completion: TextChat) =
        requestJson(true).execute(completion).mapOutput {
            try {
                AiOutput(other = JSON_MAPPER.readValue<T>(it.textContent().trim()))
            } catch (x: JsonMappingException) {
                fine<CompletionBuilder>("Failed to parse response as JSON: ${x.message}, returning null.")
                AiOutput(text = "Failed to parse response as JSON: ${x.message}")
            }
        }

    private suspend fun checkWeatherSimilarity(input: String): AiPromptTrace {
        val embeddings = embeddingModel.calculateEmbedding("is it raining snowing sunny windy in city new york", input)
        val similarity = cosineSimilarity(embeddings[0], embeddings[1])
        info<WeatherAiTaskPlanner>("Input alignment to weather: $similarity")
        if (similarity < 0.5)
            throw IllegalArgumentException("The input is not about weather.")

        return AiPromptTrace(
            modelInfo = AiModelInfo(embeddingModel.modelId),
            outputInfo = AiOutputInfo.text(input)
        )
    }

}
