package tri.promptfx.`fun`

import tri.ai.core.CompletionBuilder
import tri.ai.core.EmbeddingModel
import tri.ai.core.TextCompletion
import tri.ai.embedding.cosineSimilarity
import tri.ai.openai.jsonMapper
import tri.ai.pips.AiPlanner
import tri.ai.pips.aitask
import tri.ai.prompt.trace.AiModelInfo
import tri.ai.prompt.trace.AiOutputInfo
import tri.ai.prompt.trace.AiPromptTrace
import tri.promptfx.PromptFxGlobals.lookupPrompt
import tri.util.info

/** Uses OpenAI and a weather API to answer questions about the weather. */
class WeatherAiTaskPlanner(val completionEngine: TextCompletion, val embeddingModel: EmbeddingModel, val input: String) : AiPlanner {

    override fun plan() =
        aitask("weather-similarity-check") {
            checkWeatherSimilarity(input)
        }.aitask("weather-api-request") {
            CompletionBuilder()
                .prompt(lookupPrompt("examples-api/weather-api-request"))
                .paramsInput(input)
                .tokens(500)
                .executeJson<WeatherRequest>(completionEngine)
        }.task("weather-api") {
            weatherService.getWeather(it!!)
        }.aitask("weather-response-formatter") {
            val json = jsonMapper.writeValueAsString(it)
            CompletionBuilder()
                .prompt(lookupPrompt("examples-api/weather-response-formatter"))
                .paramsInstruct(instruct = input, input = json)
                .execute(completionEngine)
        }.plan

    private suspend fun checkWeatherSimilarity(input: String): AiPromptTrace<String> {
        val embeddings = embeddingModel.calculateEmbedding("is it raining snowing sunny windy in city new york", input)
        val similarity = cosineSimilarity(embeddings[0], embeddings[1])
        info<WeatherAiTaskPlanner>("Input alignment to weather: $similarity")
        if (similarity < 0.5)
            throw IllegalArgumentException("The input is not about weather.")

        return AiPromptTrace(
            modelInfo = AiModelInfo(embeddingModel.modelId),
            outputInfo = AiOutputInfo.output(input)
        )
    }

}