package tri.promptfx.integration

import tri.ai.core.TextCompletion
import tri.ai.embedding.EmbeddingService
import tri.ai.embedding.cosineSimilarity
import tri.ai.openai.*
import tri.ai.pips.*
import java.util.logging.Logger

/** Uses OpenAI and a weather API to answer questions about the weather. */
class WeatherAiTaskPlanner(val completionEngine: TextCompletion, val embeddingService: EmbeddingService, val input: String) : AiPlanner {

    override fun plan() =
        aitask("weather-similarity-check") {
            checkWeatherSimilarity(input)
        }.aitask("weather-api-request") {
            completionEngine.jsonPromptTask<WeatherRequest>("weather-api-request", input, tokenLimit = 500)
        }.task("weather-api") {
            weatherService.getWeather(it)
        }.aitask("weather-response-formatter") {
            val json = mapper.writeValueAsString(it)
            completionEngine.instructTask("weather-response-formatter", instruct = input, userText = json, tokenLimit = 500)
        }.plan

    private suspend fun checkWeatherSimilarity(input: String): AiTaskResult<String> {
        val embeddings = embeddingService.calculateEmbedding("is it raining snowing sunny windy in city new york", input)
        val similarity = cosineSimilarity(embeddings[0], embeddings[1])
        Logger.getLogger("WeatherAiTaskPlanner").info("Input alignment to weather: $similarity")
        if (similarity < 0.5)
            throw IllegalArgumentException("The input is not about weather.")

        return AiTaskResult.result(input, EMBEDDING_ADA)
    }

}